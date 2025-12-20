package com.example.job_processing_platform.jobservice.service;

import com.example.job_processing_platform.jobservice.dto.JobMessage;
import com.example.job_processing_platform.jobservice.dto.LogMessage;
import com.example.job_processing_platform.jobservice.entity.Job;
import com.example.job_processing_platform.jobservice.interfaces.JobManager;
import com.example.job_processing_platform.jobservice.producer.JobProducer;
import com.example.job_processing_platform.jobservice.producer.LogProducer;
import com.example.job_processing_platform.jobservice.repository.JobRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Primary
public class JobService implements JobManager {
    private final JobRepository jobRepository;
    private final JobProducer jobProducer;
    private final LogProducer logProducer;

    public JobService(JobRepository jobRepository, LogProducer logProducer, JobProducer jobProducer) {
        this.jobRepository = jobRepository;
        this.jobProducer = jobProducer;
        this.logProducer = logProducer;
    }

    @Override
    public void execute(String type, Map<String, Object> data) {
        Job job = new Job(type, data);
        Job savedJob = jobRepository.save(job);

        if (savedJob.getId() == null) {
            throw new IllegalStateException("Error creating a new job");
        }

        publishJob(savedJob);
        publishLog(savedJob.getId(), "New Job created");
    }

    @Override
    public void schedule(String type, Map<String, Object> data, Instant scheduledAt) {
        Job job = new Job(type, data, scheduledAt);
        Job savedJob = jobRepository.save(job);

        if (savedJob.getId() == null) {
            throw new IllegalStateException("Error creating a new job");
        }
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    private void publishLog(Long jobId, String message) {
        LogMessage logMessage = new LogMessage(jobId, message);
        logProducer.publish(logMessage);
    }

    private void publishJob(Job job) {
        JobMessage jobMessage = new JobMessage(job);
        jobProducer.publish(jobMessage);
    }
}