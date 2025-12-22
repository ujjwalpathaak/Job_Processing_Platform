package com.example.job_processing_platform.jobservice.producer;

import com.example.job_processing_platform.config.RabbitProperties;
import com.example.job_processing_platform.dto.JobMessage;
import com.example.job_processing_platform.interfaces.Producer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class JobProducer implements Producer<JobMessage> {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitProperties rabbitProperties;

    public JobProducer(RabbitTemplate rabbitTemplate, RabbitProperties rabbitProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitProperties = rabbitProperties;
    }

    @Override
    public void publish(JobMessage message) {
        rabbitTemplate.convertAndSend(
                rabbitProperties.getRabbit().getExchange(),
                rabbitProperties.getRabbit().getRoutingKey(),
                message
        );
    }
}