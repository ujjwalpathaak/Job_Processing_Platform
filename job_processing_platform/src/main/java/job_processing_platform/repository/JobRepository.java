package job_processing_platform.repository;

import job_processing_platform.entity.Job;
import job_processing_platform.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    @Modifying
    @Query("""
                UPDATE Job j
                SET j.status = :status
                WHERE j.id = :jobId
            """)
    int updateJobStatus(@Param("jobId") long jobId,
                        @Param("status") JobStatus status);

    List<Job> findAllByOrderByCreatedAtDesc();
}