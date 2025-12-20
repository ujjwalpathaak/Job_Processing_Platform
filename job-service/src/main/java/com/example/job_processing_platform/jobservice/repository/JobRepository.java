package com.example.job_processing_platform.jobservice.repository;

import com.example.job_processing_platform.jobservice.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
}