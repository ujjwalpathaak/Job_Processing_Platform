package job_processing_platform.service;

import jakarta.transaction.Transactional;
import job_processing_platform.config.RagProperties;
import job_processing_platform.repository.JobChunkRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JobChunkRetentionService {

    private final JobChunkRepository jobChunkRepository;
    private final RagProperties ragProperties;

    public JobChunkRetentionService(JobChunkRepository jobChunkRepository, RagProperties ragProperties) {
        this.jobChunkRepository = jobChunkRepository;
        this.ragProperties = ragProperties;
    }

    @Transactional
    public int cleanupExpiredJobChunks() {
        Instant cutoff = Instant.now().minus(ragProperties.getRetentionDays(), ChronoUnit.DAYS);
        int deleted = jobChunkRepository.deleteByCreatedAtBefore(cutoff);
        log.info("event=rag.retention.cleanup retentionDays={} deleted={}", ragProperties.getRetentionDays(), deleted);
        return deleted;
    }
}
