package com.example.job_processing_platform.jobservice.producer;

import com.example.job_processing_platform.config.JobPlatformProperties;
import com.example.job_processing_platform.dto.LogMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class LogProducer {

    private final RabbitTemplate rabbitTemplate;
    private final JobPlatformProperties properties;

    public LogProducer(
            RabbitTemplate rabbitTemplate,
            JobPlatformProperties properties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void publish(LogMessage message) {
        rabbitTemplate.convertAndSend(
                properties.getRabbit().getLogQueue(),
                properties.getRabbit().getLogRoutingKey(),
                message
        );
    }
}