package job_processing_platform.repository;

import job_processing_platform.entity.Job;
import job_processing_platform.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long>, JobRepositoryCustom {
    @Modifying
    @Query("""
                UPDATE Job j
                SET j.status = :status,
                    j.updatedAt = CURRENT_TIMESTAMP
                WHERE j.id = :jobId
            """)
    int updateJobStatus(@Param("jobId") long jobId,
                        @Param("status") JobStatus status);

    List<Job> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("""
            UPDATE Job j
            SET j.status = :newStatus,
                j.errorMessage = :error,
                j.updatedAt = CURRENT_TIMESTAMP
            WHERE j.id = :jobId
              AND j.status = :expectedStatus
            """)
    int updateStatusIfCurrentMatches(
            @Param("jobId") Long jobId,
            @Param("expectedStatus") JobStatus expectedStatus,
            @Param("newStatus") JobStatus newStatus,
            @Param("error") String error
    );

}