package job_processing_platform.service;

import jakarta.transaction.Transactional;
import job_processing_platform.dto.JobDashboardDTO;
import job_processing_platform.dto.JobHistoryDTO;
import job_processing_platform.dto.JobMessage;
import job_processing_platform.entity.Job;
import job_processing_platform.entity.JobStateHistory;
import job_processing_platform.enums.JobHandlerType;
import job_processing_platform.enums.JobStatus;
import job_processing_platform.factory.JobHandlerFactory;
import job_processing_platform.interfaces.JobHandler;
import job_processing_platform.producer.Producer;
import job_processing_platform.repository.JobRepository;
import job_processing_platform.repository.JobStatusHistoryRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Primary
public class JobService {
    private final JobRepository jobRepository;
    private final JobHandlerFactory jobHandlerFactory;
    private final Producer producer;
    private final JobStatusService jobStatusService;
    private final JobStatusHistoryRepository jobStateHistoryRepository;

    public JobService(JobRepository jobRepository, Producer producer, JobHandlerFactory jobHandlerFactory, JobStatusService jobStatusService, JobStatusHistoryRepository jobStateHistoryRepository) {
        this.jobRepository = jobRepository;
        this.jobHandlerFactory = jobHandlerFactory;
        this.jobStatusService = jobStatusService;
        this.jobStateHistoryRepository = jobStateHistoryRepository;
        this.producer = producer;
    }

    @SuppressWarnings("unchecked")
    public long execute(Map<String, Object> payload) throws Exception {
        Object typeObj = payload.get("type");
        if (typeObj == null) {
            throw new IllegalArgumentException("Missing job type");
        }
        JobHandlerType handlerType;
        try {
            handlerType = JobHandlerType.valueOf(typeObj.toString());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid job type: " + typeObj);
        }
        Object dataObj = payload.get("data");
        if (!(dataObj instanceof Map)) {
            throw new IllegalArgumentException("Missing or invalid 'data' object");
        }

        Map<String, Object> data = (Map<String, Object>) dataObj;
        Job job = null;
        try {
            JobHandler handler = jobHandlerFactory.get(handlerType);
            Job newJob = new Job(handler, data);
            job = jobRepository.save(
                    newJob
            );
            if (job.getId() == null) {
                throw new IllegalStateException("Error creating a new job");
            }
            updateJobStatus(job, JobStatus.SCHEDULED);
            JobMessage jobMessage = new JobMessage(
                    job.getId(),
                    job.getJobCategory(),
                    handlerType,
                    data
            );

            log.info(
                    "{} - CREATED NEW JOB - handler: {}, category: {}, backoffs: {}, retries: {}",
                    job.getId(),
                    handler.identify(),
                    handler.category(),
                    handler.backoff(),
                    handler.retries()
            );

            producer.publish(job, jobMessage);
        } catch (Exception ex) {
            log.error(
                    "Job creation failed for handler {} with payload {} because of error = {}",
                    handlerType,
                    payload,
                    ex.getMessage(),
                    ex
            );

            throw ex;
        }

        return job.getId();
    }

    public void updateJobStatus(Job job, JobStatus status) {
        jobStatusService.updateJobStatus(job, status, null);
    }

    public void updateJobStatus(Job job, JobStatus status, String error) {
        jobStatusService.updateJobStatus(job, status, error);
    }

    @Transactional()
    public List<JobDashboardDTO> getJobsForDashboard() {
        List<Job> jobs = jobRepository.findAllByOrderByCreatedAtDesc();

        if (jobs.isEmpty()) {
            return List.of();
        }

        List<Long> jobIds = jobs.stream()
                .map(Job::getId)
                .toList();

        List<JobStateHistory> histories =
                jobStateHistoryRepository.findByJobIdInOrderByCreatedAtAsc(jobIds);

        Map<Long, List<JobHistoryDTO>> historyByJobId =
                histories.stream()
                        .collect(Collectors.groupingBy(
                                JobStateHistory::getJobId,
                                Collectors.mapping(
                                        h -> new JobHistoryDTO(
                                                h.getStatus(),
                                                h.getCreatedAt(),
                                                h.getErrorMessage()
                                        ),
                                        Collectors.toList()
                                )
                        ));

        return jobs.stream()
                .map(job -> new JobDashboardDTO(
                        job.getId(),
                        job.getStatus(),
                        job.getJobCategory(),
                        job.getJobHandler(),
                        job.getCreatedAt(),
                        job.getData(),
                        historyByJobId.getOrDefault(job.getId(), List.of())
                ))
                .toList();
    }

    public Optional<Job> getJobById(long jobId) {
        return jobRepository.findById(jobId);
    }
}