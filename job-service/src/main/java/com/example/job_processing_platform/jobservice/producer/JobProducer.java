package com.example.job_processing_platform.jobservice.producer;

import com.example.job_processing_platform.dto.JobMessage;
import com.example.job_processing_platform.enums.JobCategory;
import com.example.job_processing_platform.interfaces.Producer;
import com.example.job_processing_platform.jobservice.manager.QueueManager;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class JobProducer implements Producer<JobMessage> {

    private final RabbitTemplate rabbitTemplate;
    private final QueueManager queueManager;

    public JobProducer(RabbitTemplate rabbitTemplate, QueueManager queueManager) {
        this.rabbitTemplate = rabbitTemplate;
        this.queueManager = queueManager;
    }

    @Override
    public void publish(JobMessage message, JobCategory jobCategory) {
        String exchange = queueManager.getExchange(jobCategory);
        String routingKey = queueManager.getRoutingKey(jobCategory);

        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                message
        );
    }
}