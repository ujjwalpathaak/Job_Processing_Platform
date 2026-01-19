package job_processing_platform.producer;

import job_processing_platform.dto.JobMessage;
import job_processing_platform.entity.Job;
import job_processing_platform.enums.JobStatus;
import job_processing_platform.manager.QueueManager;
import job_processing_platform.service.JobStatusService;
import job_processing_platform.service.log;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.naming.directory.InvalidAttributesException;

@Service
public class Producer {

    private final RabbitTemplate rabbitTemplate;
    private final QueueManager queueManager;
    private final JobStatusService jobStatusService;

    public Producer(RabbitTemplate rabbitTemplate, QueueManager queueManager, JobStatusService jobStatusService) {
        this.rabbitTemplate = rabbitTemplate;
        this.queueManager = queueManager;
        this.jobStatusService = jobStatusService;
    }

    public void publish(Job job, JobMessage message) throws RuntimeException, InvalidAttributesException {

        String exchange = queueManager.getExchange();
        String routingKey = queueManager.getRoutingKey(message.getJobCategory());
        if (exchange == null || routingKey == null) {
            throw new InvalidAttributesException("exchange and routing-key cannot be null");
        }

        try {
            rabbitTemplate.convertAndSend(
                    exchange,
                    routingKey,
                    message
            );
            jobStatusService.updateJobStatus(job, JobStatus.PUBLISHED, null);
            log.info(
                    "{} - PUBLISHED - exchange: {}, routing_key: {}",
                    message.getJobId(),
                    exchange,
                    routingKey
            );
        } catch (RuntimeException ex) {
            log.error(
                    "{} - failed to publish a job through exchange {} using routingKey {} because of the error = {}",
                    message.getJobId(),
                    exchange,
                    routingKey,
                    ex.toString()
            );
            throw ex;
        }
    }
}