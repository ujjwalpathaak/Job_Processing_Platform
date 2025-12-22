package com.example.job_processing_platform.workerservice.repository;

import com.example.job_processing_platform.workerservice.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<Log, Long> {
}