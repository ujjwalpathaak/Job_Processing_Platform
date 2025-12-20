package com.example.job_processing_platform.jobservice.service;

import com.example.job_processing_platform.jobservice.interfaces.JobManager;
import com.example.job_processing_platform.jobservice.dto.JobMessage;
import com.example.job_processing_platform.jobservice.entity.Job;
import com.example.job_processing_platform.jobservice.enums.JobType;
import com.example.job_processing_platform.jobservice.producer.JobProducer;
import com.example.job_processing_platform.jobservice.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class JobService implements JobManager {
    private final JobRepository jobRepository;
    private final JobProducer jobProducer;

    public JobService(JobRepository jobRepository, JobProducer jobProducer) {
        this.jobRepository = jobRepository;
        this.jobProducer = jobProducer;
    }

    @Override
    public void execute(String type, Map<String, Object> data) {
        JobType jobType = JobType.fromString(type);

        Job job = new Job(jobType, data);
        Job savedJob = jobRepository.save(job);

        if (savedJob.getId() == null) {
            throw new IllegalStateException("Error creating a new job");
        }

        publishJob(savedJob);
//        return;
    }

    @Override
    public void schedule(String type, Map<String, Object> data, Instant scheduledAt) {
        JobType jobType = JobType.fromString(type);

        Job job = new Job(jobType, data, scheduledAt);
        Job savedJob = jobRepository.save(job);

        if (savedJob.getId() == null) {
            throw new IllegalStateException("Error creating a new job");
        }

//        return;
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    private void publishJob(Job job) {
        JobMessage jobMessage = new JobMessage(job);
        jobProducer.publish(jobMessage);
    }
}