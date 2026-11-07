package job_processing_platform.handlers.job;

import job_processing_platform.dto.JobMessage;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.enums.JobHandlerType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebhookTriggerJobHandler extends AbstractJobHandler {

    @Override
    public JobHandlerType identify() {
        return JobHandlerType.WEBHOOK_TRIGGER;
    }

    @Override
    public JobCategory category() {
        return JobCategory.EXTERNAL;
    }

    @Override
    public int retries() {
        return 3;
    }

    @Override
    public List<String> backoff() {
        return List.of("5s", "30s", "60s");
    }

    @Override
    protected void execute(JobMessage message) {
        simulateNetworkLatency();

        if (Math.random() < 0.4) {
            throw new RuntimeException("Webhook responded with HTTP 502");
        }

        System.out.println("Webhook delivered successfully");
    }

    private void simulateNetworkLatency() {
        try {
            Thread.sleep((long) (Math.random() * 2000));
        } catch (InterruptedException ignored) {
        }
    }
}