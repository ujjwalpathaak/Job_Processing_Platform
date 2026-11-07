package job_processing_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import job_processing_platform.config.AiProperties;
import job_processing_platform.dto.RagEmbeddingMatchDTO;
import job_processing_platform.entity.RagEmbedding;
import job_processing_platform.repository.RagEmbeddingRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagEmbeddingService {

    private final RagEmbeddingRepository repository;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;
    private final HttpClient httpClient;

    public RagEmbeddingService(RagEmbeddingRepository repository, ObjectMapper objectMapper, AiProperties aiProperties) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.aiProperties = aiProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public void saveEmbedding(String sourceType, String sourceName, String content, String requestId, String embeddingJson) {
        repository.save(new RagEmbedding(sourceType, sourceName, content, requestId, embeddingJson));
    }

    public List<RagEmbeddingMatchDTO> findSimilar(String embeddingJson, int limit) {
        List<RagEmbedding> matches = repository.findNearest(embeddingJson, limit);
        return matches.stream()
                .map(item -> new RagEmbeddingMatchDTO(
                        item.getId(),
                        item.getSourceType(),
                        item.getSourceName(),
                        item.getContent(),
                        item.getRequestId(),
                        0.0
                ))
                .collect(Collectors.toList());
    }

    public String buildEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return "[]";
        }

        try {
            return embedText(text);
        } catch (Exception ex) {
            return fallbackVector(text);
        }
    }

    private String embedText(String text) throws Exception {
        String apiKey = aiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackVector(text);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", aiProperties.getEmbeddingModel());
        payload.put("input", text);

        String bodyJson = objectMapper.writeValueAsString(payload);
        String url = aiProperties.getBaseUrl().replaceAll("/$", "") + "/embeddings";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Embedding request failed with status " + response.statusCode());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = objectMapper.readValue(response.body(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) parsed.get("data");
        if (data == null || data.isEmpty()) {
            throw new IllegalStateException("Embedding response did not include data");
        }

        @SuppressWarnings("unchecked")
        List<Object> embeddingValues = (List<Object>) data.get(0).get("embedding");
        if (embeddingValues == null || embeddingValues.isEmpty()) {
            throw new IllegalStateException("Embedding response did not include embedding values");
        }

        return embeddingValues.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String fallbackVector(String text) {
        return text.chars()
                .boxed()
                .mapToDouble(c -> c)
                .limit(1536)
                .mapToObj(Double::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
