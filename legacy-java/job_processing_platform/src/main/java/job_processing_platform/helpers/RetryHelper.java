package job_processing_platform.helpers;

import job_processing_platform.interfaces.JobHandler;
import org.springframework.amqp.core.Message;

public final class RetryHelper {

    public static final String RETRY_HEADER = "x-retry-count";

    public static int getRetryCount(Message raw) {
        Object value = raw.getMessageProperties()
                .getHeaders()
                .getOrDefault(RETRY_HEADER, 0);
        return (int) value;
    }

    public static boolean shouldRetry(JobHandler handler, int retryCount) {
        return retryCount < handler.retries();
    }
}