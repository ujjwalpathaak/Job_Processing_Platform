package com.example.job_processing_platform.jobservice.producer;

import com.example.job_processing_platform.config.JobPlatformProperties;
import com.example.job_processing_platform.dto.JobMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class JobProducer {

    private final RabbitTemplate rabbitTemplate;
    private final JobPlatformProperties properties;

    public JobProducer(
            RabbitTemplate rabbitTemplate,
            JobPlatformProperties properties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void publish(JobMessage message) {
        rabbitTemplate.convertAndSend(
                properties.getRabbit().getExchange(),
                properties.getRabbit().getRoutingKey(),
                message
        );
    }
}