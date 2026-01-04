package job_processing_platform.manager;

import job_processing_platform.config.RabbitProperties;
import job_processing_platform.enums.JobCategory;
import org.springframework.stereotype.Component;

@Component
public class QueueManager {
    private final RabbitProperties props;

    public QueueManager(RabbitProperties rabbitProperties) {
        this.props = rabbitProperties;
    }

    public String getExchange() {
        return props.getRabbit().getExchanges().get("standard");
    }

    public String getRoutingKey(JobCategory jobCategory) {
        return switch (jobCategory) {
            case STANDARD -> props.getRabbit().getStandard().getRoutingKey();
            case EXTERNAL -> null;
            case CRITICAL -> null;
        };
    }
}