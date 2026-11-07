package job_processing_platform.repository;

import job_processing_platform.entity.JobStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobStatusHistoryRepository extends JpaRepository<JobStateHistory, Long> {
    List<JobStateHistory> findByJobIdInOrderByCreatedAtAsc(List<Long> jobIds);
}