package job_processing_platform.helpers;

import job_processing_platform.config.Rabbit.RabbitProperties;
import job_processing_platform.interfaces.JobHandler;
import org.springframework.amqp.core.Message;

public final class RetryHelper {

    public static final String RETRY_HEADER = "x-retry-count";

    private RetryHelper() {
    }

    public static int getRetryCount(Message message) {
        Object value = message.getMessageProperties()
                .getHeaders()
                .getOrDefault(RETRY_HEADER, 0);
        return (int) value;
    }

    public static boolean shouldRetry(JobHandler handler, int retryCount) {
        return retryCount < handler.retries();
    }

    public static RabbitProperties.Retry resolveRetryQueue(
            JobHandler handler,
            RabbitProperties props,
            int retryCount
    ) {
        String delayKey = handler.backoff().get(retryCount);
        return props.getRabbit().getRetries().get(delayKey);
    }
}