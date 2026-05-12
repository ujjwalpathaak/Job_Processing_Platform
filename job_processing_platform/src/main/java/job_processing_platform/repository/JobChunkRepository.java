package job_processing_platform.repository;

import job_processing_platform.entity.JobChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface JobChunkRepository extends JpaRepository<JobChunk, Long> {

    @Modifying
    @Query("DELETE FROM JobChunk c WHERE c.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
