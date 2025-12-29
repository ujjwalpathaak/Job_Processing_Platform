package job_processing_platform.service;

import job_processing_platform.dto.JobMessage;
import job_processing_platform.entity.Job;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.interfaces.JobHandler;
import job_processing_platform.producer.Producer;
import job_processing_platform.repository.JobRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Primary
public class JobService {
    private final JobRepository jobRepository;
    private final Producer producer;

    public JobService(JobRepository jobRepository, Producer producer) {
        this.jobRepository = jobRepository;
        this.producer = producer;
    }

    public void execute(JobHandler handler, Map<String, Object> data) {
        Job job = jobRepository.save(new Job(handler, data));
        JobCategory jobCategory = handler.definition().category();

        if (job.getId() == null) {
            throw new IllegalStateException("Error creating a new job");
        }

        publishJob(job, jobCategory);
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    private void publishJob(Job job, JobCategory jobCategory) {
        Long jobId = job.getId();
        String jobType = job.getType();
        Map<String, Object> data = job.getData();

        producer.publish(new JobMessage(jobId, jobType, data), jobCategory);
    }
}