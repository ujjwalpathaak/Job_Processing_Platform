package job_processing_platform.controller;

import job_processing_platform.dto.ApiResponse;
import job_processing_platform.dto.JobDashboardDTO;
import job_processing_platform.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/jobs")
public class CommonController {

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
    public ResponseEntity<ApiResponse<List<JobDashboardDTO>>> getJobs() {
        List<JobDashboardDTO> jobs = jobService.getJobsForDashboard();
        return ResponseEntity.ok().body(ApiResponse.success(jobs));
    }
}