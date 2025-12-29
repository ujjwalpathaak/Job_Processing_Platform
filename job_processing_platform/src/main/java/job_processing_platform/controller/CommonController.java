package job_processing_platform.controller;

import job_processing_platform.dto.EmailJobRequest;
import job_processing_platform.entity.Job;
import job_processing_platform.handlers.EmailHandler;
import job_processing_platform.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class CommonController {

    private final JobService jobService;
    private final EmailHandler emailHandler;

    public CommonController(JobService jobService, EmailHandler emailHandler) {
        this.jobService = jobService;
        this.emailHandler = emailHandler;
    }

    @PostMapping("/email")
    public ResponseEntity<Void> sendEmail(@RequestBody EmailJobRequest payload) {
        Map<String, Object> data = new HashMap<>();
        data.put("from", payload.from);
        data.put("to", payload.to);
        data.put("subject", payload.subject);
        data.put("content", payload.content);

        jobService.execute(emailHandler, data);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getJobs() {
        List<Job> jobs = jobService.getAllJobs();
        return ResponseEntity.ok().body(jobs);
    }
}