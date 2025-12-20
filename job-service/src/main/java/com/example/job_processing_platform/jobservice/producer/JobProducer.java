package com.example.job_processing_platform.jobservice.producer;

import com.example.job_processing_platform.jobservice.config.RabbitConfig;
import com.example.job_processing_platform.jobservice.dto.JobMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class JobProducer {

    private final RabbitTemplate rabbitTemplate;

    public JobProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(JobMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.getJobExchange(),
                RabbitConfig.getJobRoutingKey(),
                message
        );
    }
}