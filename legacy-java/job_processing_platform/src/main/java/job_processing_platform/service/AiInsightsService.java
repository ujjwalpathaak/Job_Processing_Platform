package job_processing_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import job_processing_platform.config.AiProperties;
import job_processing_platform.dto.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiInsightsService {

    private static final int DEFAULT_HOURS = 24;
    private static final int DEFAULT_LOG_LINES = 120;
    private static final int DEFAULT_JOB_LIMIT = 100;
    private static final int MAX_HOURS = 24 * 30;
    private static final int MAX_LOG_LINES = 1000;
    private static final int MAX_JOB_LIMIT = 500;
    private static final int TOP_LIST_SIZE = 5;
    private static final int MAX_RETRIEVED_CHUNKS = 8;
    private static final String FALLBACK_MODEL_LABEL = "rule-based-rag-ops-insights";

    private static final List<String> LOG_SOURCES = List.of(
            "logs/application.log",
            "logs/error.log",
            "logs/handler.application.log",
            "logs/handler.error.log"
    );

    private static final List<String> DOC_SOURCES = List.of(
            "README.md",
            "DASHBOARD.md",
            "SETUP.md"
    );

    private static final Pattern LOG_LINE_PATTERN =
            Pattern.compile("^\\[(.*?)]\\s+\\[(.*?)]\\s+\\[(.*?)]\\s+(.*)$");
    private static final Pattern EVENT_PATTERN = Pattern.compile("\\bevent=([^\\s]+)");
    private static final Pattern ERROR_PATTERN = Pattern.compile("(?i)\\b(?:error|reason)=([^\\n]+)");

    private final JobService jobService;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final RagEmbeddingService ragEmbeddingService;

    public AiInsightsService(JobService jobService, AiProperties aiProperties, ObjectMapper objectMapper,
                            RagEmbeddingService ragEmbeddingService) {
        this.jobService = jobService;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.ragEmbeddingService = ragEmbeddingService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    // ─── Public entry point ───────────────────────────────────────────────────

    public AiInsightsResponseDTO generateAiInsights(AiInsightsRequestDTO request) {
        NormalizedRequest norm = normalizeRequest(request);
        Instant since = Instant.now().minusSeconds((long) norm.hours * 3600);

        JobQueryOptionsDTO queryOptions = new JobQueryOptionsDTO(
                null, null, null, null, "updatedAt", "desc", norm.jobLimit, null);
        List<JobDashboardDTO> jobs = jobService.getJobsForDashboard(queryOptions, since);
        List<ParsedLogLine> logs = readRecentLogs(since, norm.logLines);

        List<AiInsightContextJobDTO> failedJobs = jobs.stream()
                .filter(j -> Set.of("ERROR", "DEAD", "RETRY").contains(j.status().name()))
                .limit(10)
                .map(this::toContextJob)
                .toList();

        AiInsightMetricsDTO metrics = buildMetrics(jobs, logs);
        List<AiRetrievedChunkDTO> retrievedChunks = retrieveRelevantChunks(norm.question, jobs, logs);

        GeneratedContent content = buildFallbackInsights(norm, metrics, failedJobs, logs, retrievedChunks);
        boolean usedAiModel = false;
        String model = FALLBACK_MODEL_LABEL;

        try {
            GeneratedContent aiContent = generateWithProvider(norm, metrics, failedJobs, retrievedChunks);
            if (aiContent != null) {
                content = new GeneratedContent(
                        aiContent.summary(),
                        aiContent.answer() != null ? aiContent.answer() : content.answer(),
                        !aiContent.findings().isEmpty() ? aiContent.findings() : content.findings(),
                        !aiContent.recommendations().isEmpty() ? aiContent.recommendations() : content.recommendations()
                );
                usedAiModel = true;
                model = aiProperties.getModel();
            }
        } catch (Exception ignored) {
            // fall through to rule-based content already set
        }

        AiInsightSourceDTO source = new AiInsightSourceDTO(
                Instant.now().toString(),
                usedAiModel,
                model,
                norm.hours,
                norm.logLines,
                norm.jobLimit,
                LOG_SOURCES,
                true,
                retrievedChunks.size()
        );

        AiInsightContextDTO context = null;
        if (norm.includeRawContext) {
            context = new AiInsightContextDTO(
                    failedJobs,
                    metrics.topErrors().stream().map(AiInsightMetricsErrorEntryDTO::message).toList(),
                    logs.stream().limit(20).map(ParsedLogLine::raw).toList(),
                    retrievedChunks
            );
        }

        return new AiInsightsResponseDTO(
                content.summary(),
                content.answer(),
                content.findings(),
                content.recommendations(),
                metrics,
                source,
                context
        );
    }

    // ─── Request normalization ─────────────────────────────────────────────────

    private NormalizedRequest normalizeRequest(AiInsightsRequestDTO req) {
        if (req == null) {
            return new NormalizedRequest(DEFAULT_HOURS, DEFAULT_LOG_LINES, DEFAULT_JOB_LIMIT, null, false);
        }
        return new NormalizedRequest(
                clamp(req.hours() != null ? req.hours() : DEFAULT_HOURS, 1, MAX_HOURS),
                clamp(req.logLines() != null ? req.logLines() : DEFAULT_LOG_LINES, 10, MAX_LOG_LINES),
                clamp(req.jobLimit() != null ? req.jobLimit() : DEFAULT_JOB_LIMIT, 1, MAX_JOB_LIMIT),
                req.question() != null && !req.question().isBlank() ? req.question().trim() : null,
                Boolean.TRUE.equals(req.includeRawContext())
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // ─── Log reading ──────────────────────────────────────────────────────────

    private List<ParsedLogLine> readRecentLogs(Instant since, int maxLines) {
        String workDir = System.getProperty("user.dir");
        int perFileLimit = Math.max(20, (int) Math.ceil((double) maxLines / Math.max(LOG_SOURCES.size(), 1)) * 2);

        List<ParsedLogLine> collected = LOG_SOURCES.stream()
                .flatMap(rel -> {
                    Path path = Paths.get(workDir, rel);
                    return readLastLines(path, perFileLimit).stream().map(this::parseLogLine);
                })
                .collect(Collectors.toList());

        return collected.stream()
                .filter(entry -> {
                    if (entry.timestamp() == null) return true;
                    try {
                        Instant ts = Instant.parse(entry.timestamp());
                        return !ts.isBefore(since);
                    } catch (DateTimeParseException ex) {
                        return true;
                    }
                })
                .sorted(Comparator.comparingLong(e -> {
                    if (e.timestamp() == null) return 0L;
                    try {
                        return -Instant.parse(e.timestamp()).toEpochMilli();
                    } catch (DateTimeParseException ex) {
                        return 0L;
                    }
                }))
                .limit(maxLines)
                .toList();
    }

    private List<String> readLastLines(Path path, int count) {
        if (!Files.exists(path)) return List.of();
        try {
            List<String> all = Files.readAllLines(path);
            List<String> trimmed = all.stream()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .toList();
            int from = Math.max(0, trimmed.size() - count);
            return trimmed.subList(from, trimmed.size());
        } catch (IOException ex) {
            return List.of();
        }
    }

    private ParsedLogLine parseLogLine(String raw) {
        Matcher m = LOG_LINE_PATTERN.matcher(raw);
        String timestamp = null;
        String level = "INFO";
        String message = raw;

        if (m.matches()) {
            timestamp = m.group(2);
            level = m.group(3).toUpperCase();
            message = m.group(4);
        }

        Matcher eventMatcher = EVENT_PATTERN.matcher(message);
        String event = eventMatcher.find() ? eventMatcher.group(1) : null;

        Matcher errorMatcher = ERROR_PATTERN.matcher(message);
        String error = errorMatcher.find() ? errorMatcher.group(1).trim() : null;

        return new ParsedLogLine(timestamp, level, message, raw, event, error);
    }

    // ─── Metrics ──────────────────────────────────────────────────────────────

    private AiInsightMetricsDTO buildMetrics(List<JobDashboardDTO> jobs, List<ParsedLogLine> logs) {
        Map<String, Integer> byStatus = countBy(jobs.stream().map(j -> j.status().name()).toList());
        Map<String, Integer> byHandler = countBy(jobs.stream().map(j -> j.jobHandler().name()).toList());
        Map<String, Integer> byCategory = countBy(jobs.stream().map(j -> j.jobCategory().name()).toList());
        Map<String, Integer> logLevels = countBy(logs.stream().map(ParsedLogLine::level).toList());

        List<AiInsightMetricsTopEntryDTO> topEvents = topEntries(
                countBy(logs.stream().map(l -> l.event() != null ? l.event() : "unknown").toList()),
                TOP_LIST_SIZE
        );

        List<AiInsightMetricsErrorEntryDTO> topErrors = buildTopErrors(logs, TOP_LIST_SIZE);

        return new AiInsightMetricsDTO(jobs.size(), logs.size(), byStatus, byHandler, byCategory,
                logLevels, topEvents, topErrors);
    }

    private Map<String, Integer> countBy(List<String> values) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String v : values) {
            String key = v != null ? v : "UNKNOWN";
            counts.merge(key, 1, Integer::sum);
        }
        return counts;
    }

    private List<AiInsightMetricsTopEntryDTO> topEntries(Map<String, Integer> counts, int size) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(size)
                .map(e -> new AiInsightMetricsTopEntryDTO(e.getKey(), e.getValue()))
                .toList();
    }

    private List<AiInsightMetricsErrorEntryDTO> buildTopErrors(List<ParsedLogLine> logs, int size) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ParsedLogLine entry : logs) {
            String key = entry.error() != null ? entry.error()
                    : ("ERROR".equals(entry.level()) ? entry.message() : "");
            if (!key.isBlank()) {
                counts.merge(key, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(size)
                .map(e -> new AiInsightMetricsErrorEntryDTO(e.getKey(), e.getValue()))
                .toList();
    }

    // ─── RAG chunking ─────────────────────────────────────────────────────────

    private List<RagChunk> buildJobChunks(List<JobDashboardDTO> jobs) {
        return jobs.stream().map(job -> {
            String error = extractJobError(job);
            String content = String.join(" | ",
                    "job " + job.id(),
                    "handler " + job.jobHandler().name(),
                    "category " + job.jobCategory().name(),
                    "status " + job.status().name(),
                    "updated " + (job.updatedAt() != null ? job.updatedAt().toString() : ""),
                    error != null ? "error " + error : "",
                    "data " + truncate(jsonOf(job.data()), 500)
            ).replace(" |  |", " |").strip();

            return new RagChunk("job-" + job.id(), "job", String.valueOf(job.id()), content, 0,
                    normalizeText(content));
        }).toList();
    }

    private List<RagChunk> buildLogChunks(List<ParsedLogLine> logs) {
        List<RagChunk> chunks = new ArrayList<>();
        for (int i = 0; i < logs.size(); i++) {
            ParsedLogLine log = logs.get(i);
            String sourceName = log.event() != null ? log.event() : log.level();
            chunks.add(new RagChunk("log-" + (i + 1), "log", sourceName, log.raw(), 0,
                    normalizeText(log.raw())));
        }
        return chunks;
    }

    private List<RagChunk> chunkDocumentText(Path filePath) {
        if (!Files.exists(filePath)) return List.of();
        String content;
        try {
            content = Files.readString(filePath);
        } catch (IOException ex) {
            return List.of();
        }
        if (content.isBlank()) return List.of();

        String[] parts = content.split("\\n\\s*\\n");
        List<RagChunk> chunks = new ArrayList<>();
        int index = 0;
        for (String part : parts) {
            if (chunks.size() >= 24) break;
            String cleaned = part
                    .replaceAll("```[\\s\\S]*?```", " ")
                    .replaceAll("[#>*`\\-]", " ")
                    .trim();
            if (cleaned.length() > 40) {
                String truncated = truncate(cleaned, 900);
                String id = "doc-" + filePath.getFileName() + "-" + (++index);
                String relPath = Paths.get(System.getProperty("user.dir")).relativize(filePath).toString();
                chunks.add(new RagChunk(id, "doc", relPath, truncated, 0, normalizeText(truncated)));
            }
        }
        return chunks;
    }

    // ─── Retrieval ────────────────────────────────────────────────────────────

    private List<AiRetrievedChunkDTO> retrieveRelevantChunks(
            String query, List<JobDashboardDTO> jobs, List<ParsedLogLine> logs) {

        String retrievalQuery = query != null && !query.isBlank()
                ? query
                : "operational health failures retries dead jobs recurring log errors dashboard issues";
        List<String> queryTokens = tokenize(retrievalQuery);

        String workDir = System.getProperty("user.dir");
        List<RagChunk> all = new ArrayList<>();
        all.addAll(buildJobChunks(jobs));
        all.addAll(buildLogChunks(logs));
        DOC_SOURCES.forEach(rel -> all.addAll(chunkDocumentText(Paths.get(workDir, rel))));

        String embeddingJson = ragEmbeddingService.buildEmbedding(retrievalQuery);
        List<RagEmbeddingMatchDTO> embeddingMatches = ragEmbeddingService.findSimilar(embeddingJson, MAX_RETRIEVED_CHUNKS);
        embeddingMatches.forEach(match -> all.add(new RagChunk(
                "emb-" + match.id(),
                "emb",
                match.sourceName(),
                match.content(),
                0,
                normalizeText(match.content())
        )));

        List<RagChunk> scored = all.stream()
                .map(chunk -> chunk.withScore(scoreChunk(retrievalQuery, queryTokens, chunk)))
                .filter(c -> c.score() > 0)
                .sorted(Comparator.comparingInt(RagChunk::score).reversed())
                .limit(MAX_RETRIEVED_CHUNKS)
                .toList();

        List<RagChunk> result = scored.isEmpty() ? fallbackChunks(all) : scored;

        return result.stream()
                .map(c -> new AiRetrievedChunkDTO(c.id(), c.sourceType(), c.sourceName(), c.content(), c.score()))
                .toList();
    }

    private int scoreChunk(String query, List<String> queryTokens, RagChunk chunk) {
        String searchText = chunk.searchText();
        if (searchText == null || searchText.isBlank()) return 0;

        int score = 0;
        Set<String> unique = new HashSet<>(queryTokens);
        for (String token : unique) {
            if (searchText.contains(token)) score += 4;
        }

        if (!query.isBlank() && searchText.contains(normalizeText(query))) score += 8;

        if ("job".equals(chunk.sourceType()) && chunk.content().matches("(?i).*(error|dead|retry).*")) score += 3;
        if ("log".equals(chunk.sourceType()) && chunk.content().matches("(?i).*(\\[ERROR]|error=|reason=).*")) score += 2;

        return score;
    }

    private List<RagChunk> fallbackChunks(List<RagChunk> all) {
        return all.stream()
                .filter(c -> {
                    if ("job".equals(c.sourceType())) return c.content().matches(".*(ERROR|DEAD|RETRY).*");
                    if ("log".equals(c.sourceType())) return c.content().matches("(?i).*(\\[ERROR]|error=|reason=).*");
                    return c.content().matches("(?i).*(dashboard|api|job|retry|error|log).*");
                })
                .limit(MAX_RETRIEVED_CHUNKS)
                .toList();
    }

    // ─── Rule-based fallback insights ─────────────────────────────────────────

    private GeneratedContent buildFallbackInsights(
            NormalizedRequest req, AiInsightMetricsDTO metrics,
            List<AiInsightContextJobDTO> failedJobs, List<ParsedLogLine> logs,
            List<AiRetrievedChunkDTO> retrievedChunks) {

        boolean healthy = (metrics.logLevels().getOrDefault("ERROR", 0) == 0) && failedJobs.isEmpty();
        String summary = healthy
                ? String.format("System looks healthy in the last %d hour(s). %d jobs and %d log lines were " +
                "reviewed with retrieved context from logs, jobs, and project docs.",
                req.hours, metrics.jobsAnalyzed(), metrics.logsAnalyzed())
                : String.format("Detected operational risk in the last %d hour(s): %d failed or retrying jobs " +
                "and %d error-level log lines across %d recent jobs.",
                req.hours, failedJobs.size(), metrics.logLevels().getOrDefault("ERROR", 0),
                metrics.jobsAnalyzed());

        List<String> findings = new ArrayList<>();
        findings.add("Job status mix: " + formatDistribution(metrics.jobsByStatus(), "No recent jobs found") + ".");
        findings.add("Top handlers: " + formatDistribution(metrics.jobsByHandler(), "No handler activity found") + ".");
        findings.add("Log levels: " + formatDistribution(metrics.logLevels(), "No logs found") + ".");

        if (!metrics.topEvents().isEmpty()) {
            String events = metrics.topEvents().stream()
                    .map(e -> e.name() + " (" + e.count() + ")").collect(Collectors.joining(", "));
            findings.add("Most frequent events: " + events + ".");
        }
        if (!metrics.topErrors().isEmpty()) {
            String errors = metrics.topErrors().stream()
                    .map(e -> e.message() + " (" + e.count() + ")").collect(Collectors.joining("; "));
            findings.add("Most common errors: " + errors + ".");
        }
        if (!failedJobs.isEmpty()) {
            String jobs = failedJobs.stream().limit(5)
                    .map(j -> j.id() + " [" + j.handler() + "/" + j.status() + "]"
                            + (j.error() != null ? " " + j.error() : ""))
                    .collect(Collectors.joining("; "));
            findings.add("Recent failed jobs: " + jobs + ".");
        }
        if (!retrievedChunks.isEmpty()) {
            String evidence = retrievedChunks.stream().limit(3)
                    .map(c -> c.sourceType() + ":" + c.sourceName()).collect(Collectors.joining(", "));
            findings.add("Retrieved evidence: " + evidence + ".");
        }
        if (req.question != null) {
            findings.add("Question received: " + req.question);
        }

        List<String> recommendations = new ArrayList<>();
        if (metrics.logLevels().getOrDefault("ERROR", 0) > 0) {
            recommendations.add("Review the recent error log cluster and map repeated errors to the affected handler or consumer.");
        }
        if (failedJobs.stream().anyMatch(j -> "DEAD".equals(j.status()))) {
            recommendations.add("Inspect DEAD jobs first because they will not recover without a manual retry or code/config fix.");
        }
        if (failedJobs.stream().anyMatch(j -> "RETRY".equals(j.status()))) {
            recommendations.add("Check retrying jobs for the same root cause before they exhaust retry limits.");
        }
        if (retrievedChunks.stream().anyMatch(c -> "doc".equals(c.sourceType()))) {
            recommendations.add("Use the retrieved project documentation context to compare expected behavior with current operational signals.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Keep monitoring the dashboard and refresh AI insights after the next deployment or workload spike.");
        }
        if (logs.isEmpty()) {
            recommendations.add("No log lines were available in the requested time window. Verify the log files exist and the app has write access to the logs directory.");
        }

        String answer = req.question != null
                ? "Using retrieved context from jobs, logs, and docs, " + summary.toLowerCase()
                + " Key signals: " + findings.stream().limit(4).collect(Collectors.joining(" "))
                : null;

        return new GeneratedContent(summary, answer, findings, recommendations);
    }

    // ─── AI provider call ─────────────────────────────────────────────────────

    private GeneratedContent generateWithProvider(
            NormalizedRequest req, AiInsightMetricsDTO metrics,
            List<AiInsightContextJobDTO> failedJobs, List<AiRetrievedChunkDTO> retrievedChunks) throws Exception {

        String apiKey = aiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) return null;

        String prompt = buildAiPrompt(req, metrics, failedJobs, retrievedChunks);

        String systemContent = (aiProperties.getSystemPrompt() != null && !aiProperties.getSystemPrompt().isBlank())
                ? aiProperties.getSystemPrompt()
                : "You produce operational insights using retrieved context from logs, jobs, and documentation.";

        Map<String, Object> requestBody = Map.of(
                "model", aiProperties.getModel(),
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemContent),
                        Map.of("role", "user", "content", prompt)
                )
        );

        String bodyJson = objectMapper.writeValueAsString(requestBody);
        String url = aiProperties.getBaseUrl().replaceAll("/$", "") + "/chat/completions";

        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        HttpResponse<String> response = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("AI provider request failed with status " + response.statusCode());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = objectMapper.readValue(response.body(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
        if (choices == null || choices.isEmpty()) return null;

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) return null;

        String content = (String) message.get("content");
        return content != null ? tryParseGeneratedContent(content) : null;
    }

    private String buildAiPrompt(NormalizedRequest req, AiInsightMetricsDTO metrics,
                                  List<AiInsightContextJobDTO> failedJobs,
                                  List<AiRetrievedChunkDTO> retrievedChunks) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("question", req.question);
            payload.put("hours", req.hours);
            payload.put("metrics", metrics);
            payload.put("failedJobs", failedJobs.stream().limit(10).toList());
            payload.put("retrievedContext", retrievedChunks);

            return String.join("\n",
                    "You are an operations AI for a job processing platform.",
                    "Use only the retrieved context and metrics provided below.",
                    "The retrieval corpus contains recent jobs, recent logs, and local project documentation.",
                    "Return strict JSON with this shape:",
                    "{\"summary\":\"string\",\"answer\":\"string optional\",\"findings\":[\"string\"],\"recommendations\":[\"string\"]}",
                    "Keep findings and recommendations concise, evidence-based, and actionable.",
                    objectMapper.writeValueAsString(payload)
            );
        } catch (Exception ex) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private GeneratedContent tryParseGeneratedContent(String value) {
        String trimmed = value.trim();
        java.util.regex.Matcher fence = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE)
                .matcher(trimmed);
        String candidate = fence.find() ? fence.group(1).trim() : trimmed;

        try {
            Map<String, Object> parsed = objectMapper.readValue(candidate, Map.class);
            if (!(parsed.get("summary") instanceof String)) return null;

            String summary = (String) parsed.get("summary");
            String answer = parsed.get("answer") instanceof String ? (String) parsed.get("answer") : null;
            List<String> findings = parsed.get("findings") instanceof List<?>
                    ? ((List<?>) parsed.get("findings")).stream()
                    .filter(s -> s instanceof String).map(s -> (String) s).toList()
                    : List.of();
            List<String> recommendations = parsed.get("recommendations") instanceof List<?>
                    ? ((List<?>) parsed.get("recommendations")).stream()
                    .filter(s -> s instanceof String).map(s -> (String) s).toList()
                    : List.of();

            return new GeneratedContent(summary, answer, findings, recommendations);
        } catch (Exception ex) {
            return null;
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String extractJobError(JobDashboardDTO job) {
        if (job.history() != null) {
            return job.history().stream()
                    .filter(h -> h.error() != null && !h.error().isBlank())
                    .reduce((a, b) -> b)
                    .map(JobHistoryDTO::error)
                    .orElse(null);
        }
        if (job.data() != null && job.data().get("error") instanceof String s && !s.isBlank()) {
            return s;
        }
        return null;
    }

    private AiInsightContextJobDTO toContextJob(JobDashboardDTO job) {
        return new AiInsightContextJobDTO(
                String.valueOf(job.id()),
                job.jobHandler().name(),
                job.jobCategory().name(),
                job.status().name(),
                job.updatedAt() != null ? job.updatedAt().toString() : null,
                extractJobError(job)
        );
    }

    private String formatDistribution(Map<String, Integer> dist, String emptyText) {
        if (dist == null || dist.isEmpty()) return emptyText;
        return dist.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
    }

    private String normalizeText(String value) {
        if (value == null) return "";
        return value.toLowerCase().replaceAll("[^a-z0-9\\s:_\\-]", " ").replaceAll("\\s+", " ").trim();
    }

    private List<String> tokenize(String value) {
        return Arrays.stream(normalizeText(value).split(" "))
                .filter(t -> t.length() > 1)
                .toList();
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return "";
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    private String jsonOf(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    // ─── Private types ────────────────────────────────────────────────────────

    private record ParsedLogLine(String timestamp, String level, String message,
                                  String raw, String event, String error) {
    }

    private record NormalizedRequest(int hours, int logLines, int jobLimit,
                                      String question, boolean includeRawContext) {
    }

    private record RagChunk(String id, String sourceType, String sourceName,
                             String content, int score, String searchText) {
        RagChunk withScore(int newScore) {
            return new RagChunk(id, sourceType, sourceName, content, newScore, searchText);
        }
    }

    private record GeneratedContent(String summary, String answer,
                                     List<String> findings, List<String> recommendations) {
    }
}
