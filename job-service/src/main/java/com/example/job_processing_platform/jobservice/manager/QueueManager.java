package com.example.job_processing_platform.jobservice.manager;

import com.example.job_processing_platform.config.RabbitProperties;
import com.example.job_processing_platform.enums.JobCategory;
import org.springframework.stereotype.Component;

@Component
public class QueueManager {
    private final RabbitProperties rabbitProperties;

    public QueueManager(RabbitProperties rabbitProperties) {
        this.rabbitProperties = rabbitProperties;
    }

    public String getExchange(JobCategory jobCategory) {
        return rabbitProperties.getRabbit().getExchange();
    }

    public String getRoutingKey(JobCategory jobCategory) {
        return switch (jobCategory) {
            case FAST -> rabbitProperties.getRabbit().getFast().getRoutingKey();
            case SLOW -> rabbitProperties.getRabbit().getSlow().getRoutingKey();
            case CRITICAL -> rabbitProperties.getRabbit().getCritical().getRoutingKey();
            case LOG -> rabbitProperties.getRabbit().getLogRoutingKey();
        };
    }
}