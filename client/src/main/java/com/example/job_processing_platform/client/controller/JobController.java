package com.example.job_processing_platform.client.controller;

import com.example.job_processing_platform.client.dto.CreateJobRequest;
import com.example.job_processing_platform.jobservice.interfaces.JobManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/job")
public class JobController {

    private final JobManager jobManager;

    public JobController(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @PostMapping
    public ResponseEntity<Void> createJob(@RequestBody CreateJobRequest payload) {
        Map<String, Object> data = new HashMap<>();
        jobManager.execute(payload.type, data);
        return ResponseEntity.ok().build();
    }
}