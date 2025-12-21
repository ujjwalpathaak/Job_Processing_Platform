package com.example.job_processing_platform.client.controller;

import com.example.job_processing_platform.client.dto.EmailJobRequest;
import com.example.job_processing_platform.client.handlers.EmailHandler;
import com.example.job_processing_platform.jobservice.entity.Job;
import com.example.job_processing_platform.jobservice.service.JobService;
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

    private final JobService jobManager;
    private final EmailHandler emailHandler;

    public CommonController(JobService jobManager, EmailHandler emailHandler) {
        this.jobManager = jobManager;
        this.emailHandler = emailHandler;
    }

    @PostMapping("/email")
    public ResponseEntity<Void> sendEmail(@RequestBody EmailJobRequest payload) {
        Map<String, Object> data = new HashMap<>();
        data.put("from", payload.from);
        data.put("to", payload.to);
        data.put("subject", payload.subject);
        data.put("content", payload.content);

        jobManager.execute(emailHandler.identify(), data);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getJobs() {
        List<Job> jobs = jobManager.getAllJobs();
        return ResponseEntity.ok().body(jobs);
    }
}