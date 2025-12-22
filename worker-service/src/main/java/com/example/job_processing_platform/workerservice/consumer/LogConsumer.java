package com.example.job_processing_platform.workerservice.consumer;

import com.example.job_processing_platform.dto.LogMessage;
import com.example.job_processing_platform.workerservice.service.LogService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class LogConsumer {
    private final LogService logService;

    public LogConsumer(LogService logService) {
        this.logService = logService;
    }

    @RabbitListener(
            queues = "${job.platform.rabbit.log-queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consume(LogMessage message) {
        logService.createLog(message.getJobId(), message.getJobType(), message.getMessage());
    }
}
