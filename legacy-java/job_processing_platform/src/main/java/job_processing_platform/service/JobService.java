package job_processing_platform.service;

import jakarta.transaction.Transactional;
import job_processing_platform.dto.DashboardJobsMetaDTO;
import job_processing_platform.dto.JobDashboardDTO;
import job_processing_platform.dto.JobHistoryDTO;
import job_processing_platform.dto.JobMessage;
import job_processing_platform.dto.JobQueryOptionsDTO;
import job_processing_platform.dto.PaginatedJobsDTO;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Primary
public class JobService {
    private static final Map<JobStatus, Set<JobStatus>> ALLOWED_TRANSITIONS =
            Map.of(
                    JobStatus.SCHEDULED, Set.of(JobStatus.PUBLISHED, JobStatus.DEAD),
                    JobStatus.PUBLISHED, Set.of(JobStatus.PROCESSING, JobStatus.DEAD),
                    JobStatus.PROCESSING, Set.of(JobStatus.PROCESSED, JobStatus.RETRY, JobStatus.ERROR, JobStatus.DEAD),
                    JobStatus.RETRY, Set.of(JobStatus.PROCESSING, JobStatus.DEAD),
                    JobStatus.ERROR, Set.of(JobStatus.RETRY, JobStatus.DEAD),
                    JobStatus.PROCESSED, Set.of(),
                    JobStatus.DEAD, Set.of()
            );

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

    public void updateJobStatus(Job job, JobStatus newStatus, String error) {

        JobStatus expected = job.getStatus();

        if (!ALLOWED_TRANSITIONS.get(expected).contains(newStatus)) {
            throw new IllegalStateException("Invalid transition");
        }

        int updated = jobRepository.updateStatusIfCurrentMatches(
                job.getId(),
                expected,
                newStatus,
                error
        );

        if (updated == 0) {
            log.info(
                    "Skipping status update because job {} moved from {} already",
                    job.getId(), expected
            );
            return;
        }
    }

    @Transactional()
    public List<JobDashboardDTO> getJobsForDashboard() {
        return getJobsForDashboard(JobQueryOptionsDTO.empty(), null);
    }

    @Transactional
    public List<JobDashboardDTO> getJobsForDashboard(JobQueryOptionsDTO options, Instant since) {
        List<Job> jobs = jobRepository.findWithOptions(options, since);

        if (jobs.isEmpty()) {
            return List.of();
        }

        return buildDashboardDTOs(jobs);
    }

    @Transactional
    public PaginatedJobsDTO getJobsForDashboardPaginated(JobQueryOptionsDTO options, int page, int limit) {
        JobQueryOptionsDTO pagedOptions = new JobQueryOptionsDTO(
                options.handler(),
                options.status(),
                options.category(),
                options.search(),
                options.sortBy(),
                options.sortOrder(),
                limit,
                (page - 1) * limit
        );

        List<Job> jobs = jobRepository.findWithOptions(pagedOptions, null);
        long total = jobRepository.countWithOptions(options, null);
        return buildPaginatedResult(jobs, total, page, limit);
    }

    @Transactional
    public PaginatedJobsDTO getUpdatedJobsPaginated(Instant since, JobQueryOptionsDTO options, int page, int limit) {
        JobQueryOptionsDTO pagedOptions = new JobQueryOptionsDTO(
                options.handler(),
                options.status(),
                options.category(),
                options.search(),
                options.sortBy(),
                options.sortOrder(),
                limit,
                (page - 1) * limit
        );

        List<Job> jobs = jobRepository.findWithOptions(pagedOptions, since);
        long total = jobRepository.countWithOptions(options, since);
        return buildPaginatedResult(jobs, total, page, limit);
    }

    @Transactional
    public Optional<JobDashboardDTO> getJobDashboardById(long jobId) {
        return jobRepository.findById(jobId).map(job -> buildDashboardDTOs(List.of(job)).get(0));
    }

    public Optional<Job> getJobById(long jobId) {
        return jobRepository.findById(jobId);
    }

    private List<JobDashboardDTO> buildDashboardDTOs(List<Job> jobs) {
        List<Long> jobIds = jobs.stream().map(Job::getId).toList();

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
                        job.getUpdatedAt(),
                        job.getData(),
                        historyByJobId.getOrDefault(job.getId(), List.of())
                ))
                .toList();
    }

    private PaginatedJobsDTO buildPaginatedResult(List<Job> jobs, long total, int page, int limit) {
        List<JobDashboardDTO> items = jobs.isEmpty() ? List.of() : buildDashboardDTOs(jobs);
        int totalPages = (int) Math.max(1, Math.ceil((double) total / limit));
        DashboardJobsMetaDTO meta = new DashboardJobsMetaDTO(total, page, limit, totalPages, page < totalPages);
        return new PaginatedJobsDTO(items, meta);
    }
}