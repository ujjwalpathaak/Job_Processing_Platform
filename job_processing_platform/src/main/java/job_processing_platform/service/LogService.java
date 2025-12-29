package job_processing_platform.service;

import job_processing_platform.entity.Log;
import job_processing_platform.repository.LogRepository;
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

    public void updateLog() {
//        Log newLog = new Log(jobId, jobType, message);
//        logRepository.
        return;
    }
}