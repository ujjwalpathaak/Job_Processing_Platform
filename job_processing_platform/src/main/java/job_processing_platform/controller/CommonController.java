package job_processing_platform.controller;

import job_processing_platform.dto.ApiResponse;
import job_processing_platform.dto.JobDashboardDTO;
import job_processing_platform.dto.JobQueryOptionsDTO;
import job_processing_platform.dto.PaginatedJobsDTO;
import job_processing_platform.service.JobService;
import job_processing_platform.service.log;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/jobs")
public class CommonController {

    private static final List<String> ALLOWED_SORT_BY =
            List.of("createdAt", "updatedAt", "status", "jobHandler", "jobCategory");
    private static final List<String> ALLOWED_SORT_ORDER = List.of("asc", "desc");

    private final JobService jobService;

    public CommonController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/new")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody Map<String, Object> payload) throws Exception {
        Long jobId = jobService.execute(payload);
        return ResponseEntity.ok().body(ApiResponse.success("Job is queued", jobId));
    }

    @GetMapping
    public ResponseEntity<?> getJobs(
            @RequestParam(required = false) String handler,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        log.info("event=api.job.list_received page={}, limit={}", page, limit);

        ParsedQueryParams parsed = parseQueryParams(handler, status, category, search, sortBy, sortOrder, page, limit);
        if (parsed.error() != null) {
            log.error("event=api.job.list_validation_failed reason={}", parsed.error());
            return ResponseEntity.badRequest().body(ApiResponse.failure(parsed.error()));
        }

        if (parsed.page() != null && parsed.limit() != null) {
            PaginatedJobsDTO result = jobService.getJobsForDashboardPaginated(parsed.options(), parsed.page(), parsed.limit());
            log.info("event=api.job.list_succeeded mode=paginated total={}", result.meta().total());
            return ResponseEntity.ok(ApiResponse.success("Jobs fetched successfully", result));
        }

        List<JobDashboardDTO> jobs = jobService.getJobsForDashboard(parsed.options(), null);
        log.info("event=api.job.list_succeeded mode=full count={}", jobs.size());
        return ResponseEntity.ok(ApiResponse.success("Jobs fetched successfully", jobs));
    }

    @GetMapping("/updates")
    public ResponseEntity<?> getJobsUpdates(
            @RequestParam String since,
            @RequestParam(required = false) String handler,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        log.info("event=api.job.updates_received since={}", since);

        if (since == null || since.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("Query param 'since' is required"));
        }

        Instant sinceInstant;
        try {
            sinceInstant = Instant.parse(since);
        } catch (DateTimeParseException ex) {
            log.error("event=api.job.updates_validation_failed reason=invalid_since since={}", since);
            return ResponseEntity.badRequest().body(ApiResponse.failure("Invalid 'since' timestamp"));
        }

        ParsedQueryParams parsed = parseQueryParams(handler, status, category, search, sortBy, sortOrder, page, limit);
        if (parsed.error() != null) {
            log.error("event=api.job.updates_validation_failed reason={}", parsed.error());
            return ResponseEntity.badRequest().body(ApiResponse.failure(parsed.error()));
        }

        if (parsed.page() != null && parsed.limit() != null) {
            PaginatedJobsDTO result = jobService.getUpdatedJobsPaginated(sinceInstant, parsed.options(), parsed.page(), parsed.limit());
            log.info("event=api.job.updates_succeeded mode=paginated total={}", result.meta().total());
            return ResponseEntity.ok(ApiResponse.success("Updated jobs fetched successfully", result));
        }

        List<JobDashboardDTO> jobs = jobService.getJobsForDashboard(parsed.options(), sinceInstant);
        log.info("event=api.job.updates_succeeded mode=full count={}", jobs.size());
        return ResponseEntity.ok(ApiResponse.success("Updated jobs fetched successfully", jobs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJobById(@PathVariable long id) {
        log.info("event=api.job.detail_received jobId={}", id);

        Optional<JobDashboardDTO> job = jobService.getJobDashboardById(id);
        if (job.isEmpty()) {
            log.error("event=api.job.detail_not_found jobId={}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure("Job not found"));
        }

        log.info("event=api.job.detail_succeeded jobId={}", id);
        return ResponseEntity.ok(ApiResponse.success("Job fetched successfully", job.get()));
    }

    private ParsedQueryParams parseQueryParams(
            String handler, String status, String category, String search,
            String sortBy, String sortOrder, Integer page, Integer limit
    ) {
        if (sortBy != null && !ALLOWED_SORT_BY.contains(sortBy)) {
            return ParsedQueryParams.error("Invalid 'sortBy'. Allowed values: " + String.join(", ", ALLOWED_SORT_BY));
        }
        if (sortOrder != null && !ALLOWED_SORT_ORDER.contains(sortOrder.toLowerCase())) {
            return ParsedQueryParams.error("Invalid 'sortOrder'. Allowed values: " + String.join(", ", ALLOWED_SORT_ORDER));
        }
        if (page != null && page < 1) {
            return ParsedQueryParams.error("Invalid 'page'. It must be an integer greater than or equal to 1");
        }
        if (limit != null && (limit < 1 || limit > 200)) {
            return ParsedQueryParams.error("Invalid 'limit'. It must be an integer between 1 and 200");
        }
        if (page != null && limit == null) {
            return ParsedQueryParams.error("Query param 'limit' is required when 'page' is provided");
        }

        Integer offset = (page != null && limit != null) ? (page - 1) * limit : null;

        JobQueryOptionsDTO options = new JobQueryOptionsDTO(
                blankToNull(handler),
                blankToNull(status),
                blankToNull(category),
                blankToNull(search),
                sortBy,
                sortOrder != null ? sortOrder.toLowerCase() : null,
                limit,
                offset
        );

        return new ParsedQueryParams(null, options, page, limit);
    }

    private static String blankToNull(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

    private record ParsedQueryParams(String error, JobQueryOptionsDTO options, Integer page, Integer limit) {
        static ParsedQueryParams error(String msg) {
            return new ParsedQueryParams(msg, null, null, null);
        }
    }
}