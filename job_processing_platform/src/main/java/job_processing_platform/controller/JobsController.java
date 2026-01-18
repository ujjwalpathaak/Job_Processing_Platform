package job_processing_platform.controller;

import job_processing_platform.dto.EmailJobRequest;
import job_processing_platform.dto.JobDashboardDTO;
import job_processing_platform.enums.JobHandlerType;
import job_processing_platform.service.JobService;
import job_processing_platform.service.log;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
public class JobsController {

    private final JobService jobService;

    public JobsController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/new/email")
    public ResponseEntity<Void> sendEmail(@RequestBody EmailJobRequest payload) {
        log.info("Received email job request: {}", payload);
        jobService.execute(JobHandlerType.EMAIL, payload);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<JobDashboardDTO>> getJobs() {
        List<JobDashboardDTO> jobs = jobService.getJobsForDashboard();
        return ResponseEntity.ok().body(jobs);
    }
}