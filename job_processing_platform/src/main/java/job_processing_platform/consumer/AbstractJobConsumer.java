package job_processing_platform.consumer;

import com.rabbitmq.client.Channel;
import jakarta.persistence.EntityNotFoundException;
import job_processing_platform.config.RabbitProperties;
import job_processing_platform.dto.JobMessage;
import job_processing_platform.entity.Job;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.enums.JobStatus;
import job_processing_platform.factory.JobHandlerFactory;
import job_processing_platform.helpers.RetryHelper;
import job_processing_platform.interfaces.JobHandler;
import job_processing_platform.service.JobService;
import job_processing_platform.service.log;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public abstract class AbstractJobConsumer {

    protected final RabbitTemplate rabbitTemplate;
    protected final RabbitProperties rabbitProperties;
    protected final JobService jobService;
    private final JobHandlerFactory jobHandlerFactory;

    public AbstractJobConsumer(
            RabbitTemplate rabbitTemplate,
            RabbitProperties rabbitProperties, JobHandlerFactory jobHandlerFactory,
            JobService jobService
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitProperties = rabbitProperties;
        this.jobHandlerFactory = jobHandlerFactory;
        this.jobService = jobService;
    }

    protected void consumeInternal(
            JobMessage jobMessage,
            Message raw,
            Channel channel
    ) throws Exception {
        JobCategory category = jobMessage.getJobCategory();
        RabbitProperties.Queue queueConfig = rabbitProperties.getRabbit().getQueue(category);
        String exchange = rabbitProperties.getRabbit().getExchanges().get(category);
        Job job = jobService.getJobById(jobMessage.getJobId()).orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobMessage.getJobId()));
        long tag = raw.getMessageProperties().getDeliveryTag();
        JobHandler handler = jobHandlerFactory.get(job.getJobHandler());

        jobService.updateJobStatus(job, JobStatus.PROCESSING);

        log.info("{}Consumer starting to process | job_id: {}", category, job.getId());

        try {
            handler.process(jobMessage);
            jobService.updateJobStatus(job, JobStatus.PROCESSED);

            channel.basicAck(tag, false);

            log.info("{}Consumer finished processing | job_id: {}", category, job.getId());
        } catch (Exception ex) {
            jobService.updateJobStatus(job, JobStatus.ERROR, ex.getMessage());

            log.info("{}Consumer failed to process | job_id: {}", category, job.getId());

            int retryCount = RetryHelper.getRetryCount(raw);
            boolean shouldRetry = RetryHelper.shouldRetry(handler, retryCount);
            if (!shouldRetry) {
                rabbitTemplate.convertAndSend(
                        exchange,
                        queueConfig.getDlqRoutingKey(),
                        jobMessage
                );
                channel.basicAck(tag, false);
                jobService.updateJobStatus(job, JobStatus.DEAD);

                log.info("{}JobConsumer marked job: {} as DEAD and moved to {} | error: {}", category, job.getId(), queueConfig.getDlqRoutingKey(), ex.toString());
                return;
            }

            String delayKey = handler.backoff().get(retryCount);
            RabbitProperties.Retry retry = this.rabbitProperties.getRabbit().getRetries().get(delayKey);
            String retryQueue = retry.getQueue();
            String retryRoutingKey = retry.getRoutingKey();

            rabbitTemplate.convertAndSend(
                    "",                 // 🔥 DEFAULT EXCHANGE
                    retryQueue,         // 🔥 QUEUE NAME (not routingKey)
                    jobMessage,
                    m -> {
                        m.getMessageProperties()
                                .getHeaders()
                                .put(RetryHelper.RETRY_HEADER, retryCount + 1);
                        return m;
                    }
            );
            channel.basicAck(tag, false);

            jobService.updateJobStatus(job, JobStatus.RETRY);

            log.info("{}JobConsumer moved job: {} to retry queue: {} | error: {}", category, job.getId(), retryQueue, ex.toString());
        }
    }
}