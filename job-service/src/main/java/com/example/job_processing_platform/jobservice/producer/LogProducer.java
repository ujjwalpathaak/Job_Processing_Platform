package com.example.job_processing_platform.jobservice.producer;

import com.example.job_processing_platform.config.RabbitProperties;
import com.example.job_processing_platform.dto.LogMessage;
import com.example.job_processing_platform.enums.JobCategory;
import com.example.job_processing_platform.interfaces.Producer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class LogProducer implements Producer<LogMessage> {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitProperties rabbitProperties;

    public LogProducer(RabbitTemplate rabbitTemplate, RabbitProperties rabbitProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitProperties = rabbitProperties;
    }

    @Override
    public void publish(LogMessage message, JobCategory jobCategory) {
        rabbitTemplate.convertAndSend(
                rabbitProperties.getRabbit().getExchange(),
                rabbitProperties.getRabbit().getLogRoutingKey(),
                message
        );
    }
}