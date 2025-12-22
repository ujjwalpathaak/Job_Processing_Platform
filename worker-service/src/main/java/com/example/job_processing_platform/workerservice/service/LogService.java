package com.example.job_processing_platform.workerservice.service;

import com.example.job_processing_platform.workerservice.entity.Log;
import com.example.job_processing_platform.workerservice.repository.LogRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
public class LogService {
    private final LogRepository logRepository;

    public LogService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public List<Log> getAllLogs() {
        return logRepository.findAll();
    }

    public void createLog(Long jobId, String jobType, String message) {
        Log newLog = new Log(jobId, jobType, message);
        logRepository.save(newLog);
    }
}