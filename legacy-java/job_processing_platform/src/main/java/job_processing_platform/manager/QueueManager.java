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

    public String getExchange(JobCategory jobCategory) {
        return switch (jobCategory) {
            case STANDARD -> props.getRabbit().getExchanges().get(JobCategory.STANDARD);
            case EXTERNAL -> props.getRabbit().getExchanges().get(JobCategory.EXTERNAL);
            case CRITICAL -> props.getRabbit().getExchanges().get(JobCategory.CRITICAL);
        };
    }

    public String getRoutingKey(JobCategory jobCategory) {
        return switch (jobCategory) {
            case STANDARD -> props.getRabbit().getStandard().getRoutingKey();
            case EXTERNAL -> props.getRabbit().getExternal().getRoutingKey();
            case CRITICAL -> props.getRabbit().getCritical().getRoutingKey();
        };
    }
}