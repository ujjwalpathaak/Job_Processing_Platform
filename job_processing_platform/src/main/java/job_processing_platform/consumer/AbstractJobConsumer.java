package job_processing_platform.consumer;

import com.rabbitmq.client.Channel;
import job_processing_platform.config.RabbitProperties;
import job_processing_platform.dto.JobMessage;
import job_processing_platform.interfaces.JobHandler;
import job_processing_platform.utils.JobHandlerRegistry;
import job_processing_platform.utils.RetryHelper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public abstract class AbstractJobConsumer {

    protected final RabbitTemplate rabbitTemplate;
    protected final RabbitProperties rabbitProperties;
    protected final JobHandlerRegistry jobHandlerRegistry;

    public AbstractJobConsumer(
            RabbitTemplate rabbitTemplate,
            RabbitProperties rabbitProperties,
            JobHandlerRegistry jobHandlerRegistry
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitProperties = rabbitProperties;
        this.jobHandlerRegistry = jobHandlerRegistry;
    }

    protected void consumeInternal(
            JobMessage job,
            Message raw,
            Channel channel,
            RabbitProperties.Queue queueConfig,
            String exchange
    ) throws Exception {

        long tag = raw.getMessageProperties().getDeliveryTag();
        JobHandler handler = jobHandlerRegistry.get(job.getJobType());

        try {
            handler.process(job);
            channel.basicAck(tag, false);

        } catch (Exception ex) {

            int retryCount = RetryHelper.getRetryCount(raw);

            if (!RetryHelper.shouldRetry(handler, retryCount)) {
                rabbitTemplate.convertAndSend(
                        exchange,
                        queueConfig.getDlqRoutingKey(),
                        job
                );
                channel.basicAck(tag, false);
                return;
            }

            RabbitProperties.Retry retry =
                    RetryHelper.resolveRetryQueue(handler, rabbitProperties, retryCount);

            rabbitTemplate.convertAndSend(
                    retry.getQueue(),
                    retry.getRoutingKey(),
                    job,
                    m -> {
                        m.getMessageProperties().getHeaders()
                                .put(RetryHelper.RETRY_HEADER, retryCount + 1);
                        return m;
                    }
            );

            channel.basicAck(tag, false);
        }
    }
}