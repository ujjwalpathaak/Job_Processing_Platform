package com.example.job_processing_platform.client.controller;

import com.example.job_processing_platform.client.dto.EmailJobRequest;
import com.example.job_processing_platform.client.handlers.EmailHandler;
import com.example.job_processing_platform.jobservice.entity.Job;
import com.example.job_processing_platform.jobservice.service.JobService;
import com.example.job_processing_platform.workerservice.entity.Log;
import com.example.job_processing_platform.workerservice.service.LogService;
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
    private final LogService logService;
    private final EmailHandler emailHandler;

    public CommonController(JobService jobService, LogService logService, EmailHandler emailHandler) {
        this.jobService = jobService;
        this.logService = logService;
        this.emailHandler = emailHandler;
    }

    @PostMapping("/email")
    public ResponseEntity<Void> sendEmail(@RequestBody EmailJobRequest payload) {
        Map<String, Object> data = new HashMap<>();
        data.put("from", payload.from);
        data.put("to", payload.to);
        data.put("subject", payload.subject);
        data.put("content", payload.content);

        jobService.execute(emailHandler.identify(), data);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getJobs() {
        List<Job> jobs = jobService.getAllJobs();
        return ResponseEntity.ok().body(jobs);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<Log>> getLogs() {
        List<Log> logs = logService.getAllLogs();
        return ResponseEntity.ok().body(logs);
    }
}