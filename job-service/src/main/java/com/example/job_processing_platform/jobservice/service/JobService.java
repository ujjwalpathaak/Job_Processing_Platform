package com.example.job_processing_platform.jobservice.service;

import com.example.job_processing_platform.dto.JobMessage;
import com.example.job_processing_platform.dto.LogMessage;
import com.example.job_processing_platform.enums.JobCategory;
import com.example.job_processing_platform.interfaces.JobHandler;
import com.example.job_processing_platform.jobservice.entity.Job;
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
public class JobService {
    private final JobRepository jobRepository;
    private final JobProducer jobProducer;
    private final LogProducer logProducer;

    public JobService(JobRepository jobRepository, LogProducer logProducer, JobProducer jobProducer) {
        this.jobRepository = jobRepository;
        this.jobProducer = jobProducer;
        this.logProducer = logProducer;
    }

    public void execute(JobHandler handler, Map<String, Object> data) {
        Job job = new Job(handler, data);
        Job savedJob = jobRepository.save(job);
        JobCategory jobCategory = handler.definition().category();

        if (savedJob.getId() == null) {
            throw new IllegalStateException("Error creating a new job");
        }

        publishJob(savedJob, jobCategory);
        publishLog(savedJob.getId(), savedJob.getType(), "New Job created");
    }

    public void schedule(JobHandler handler, Map<String, Object> data, Instant scheduledAt) {
        Job job = new Job(handler, data, scheduledAt);
        Job savedJob = jobRepository.save(job);

        if (savedJob.getId() == null) {
            throw new IllegalStateException("Error creating a new job");
        }
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    private void publishLog(Long jobId, String jobType, String message) {
        LogMessage logMessage = new LogMessage(jobId, jobType, message);
        logProducer.publish(logMessage, JobCategory.LOG);
    }

    private void publishJob(Job job, JobCategory jobCategory) {
        Long jobId = job.getId();
        String jobType = job.getType();
        Map<String, Object> data = job.getData();

        JobMessage jobMessage = new JobMessage(jobId, jobType, data);
        jobProducer.publish(jobMessage, jobCategory);
    }
}