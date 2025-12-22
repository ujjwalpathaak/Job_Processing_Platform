package com.example.job_processing_platform.workerservice.consumer;

import com.example.job_processing_platform.dto.LogMessage;
import com.example.job_processing_platform.interfaces.Consumer;
import com.example.job_processing_platform.workerservice.service.LogService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LogConsumer implements Consumer<LogMessage> {
    private final LogService logService;

    public LogConsumer(LogService logService) {
        this.logService = logService;
    }

    @RabbitListener(
            queues = "${job.platform.rabbit.log-queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )

    public void consume(LogMessage message, Channel channel, Message raw) throws IOException {
        logService.createLog(message.getJobId(), message.getJobType(), message.getMessage());
    }
}
