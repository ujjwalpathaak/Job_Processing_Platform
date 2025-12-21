package com.example.job_processing_platform.workerservice.consumer;

import com.example.job_processing_platform.dto.LogMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class LogConsumer {
    public LogConsumer() {
    }

    @RabbitListener(
            queues = "${job.platform.rabbit.log-queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consume(LogMessage message) {
        System.out.println(message);
    }
}
