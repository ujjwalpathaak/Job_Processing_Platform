package com.example.job_processing_platform.jobservice.producer;

import com.example.job_processing_platform.config.Properties;
import com.example.job_processing_platform.dto.JobMessage;
import com.example.job_processing_platform.interfaces.Producer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class JobProducer implements Producer<JobMessage> {

    private final RabbitTemplate rabbitTemplate;
    private final Properties properties;

    public JobProducer(RabbitTemplate rabbitTemplate, Properties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(JobMessage message) {
        rabbitTemplate.convertAndSend(
                properties.getRabbit().getExchange(),
                properties.getRabbit().getRoutingKey(),
                message
        );
    }
}