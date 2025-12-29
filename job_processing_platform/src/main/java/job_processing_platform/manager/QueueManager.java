package job_processing_platform.manager;

import job_processing_platform.config.RabbitProperties;
import job_processing_platform.enums.JobCategory;
import org.springframework.stereotype.Component;

@Component
public class QueueManager {
    private final RabbitProperties rabbitProperties;

    public QueueManager(RabbitProperties rabbitProperties) {
        this.rabbitProperties = rabbitProperties;
    }

    public String getExchange(JobCategory jobCategory) {
        return rabbitProperties.getRabbit().getStandardExchange();
    }

    public String getRoutingKey(JobCategory jobCategory) {
        return switch (jobCategory) {
            case STANDARD -> rabbitProperties.getRabbit().getStandard().getRoutingKey();
//            case EXTERNAL -> rabbitProperties.getRabbit().getSlow().getRoutingKey();
//            case CRITICAL -> rabbitProperties.getRabbit().getCritical().getRoutingKey();
            case EXTERNAL -> null;
            case CRITICAL -> null;
        };
    }
}