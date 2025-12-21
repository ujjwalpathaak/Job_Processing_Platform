package com.example.job_processing_platform.workerservice.consumer;

import com.example.job_processing_platform.dto.JobMessage;
import com.example.job_processing_platform.interfaces.JobHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobConsumer {
    private final List<JobHandler> handlers;

    public JobConsumer(List<JobHandler> handlers) {
        this.handlers = handlers;
    }

    @RabbitListener(
            queues = "${job.platform.rabbit.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consume(JobMessage message) {
        JobHandler handler = handlers.stream()
                .filter(h -> h.identify().equals(message.getJobType()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No JobHandler found for type: " + message.getJobType()
                        )
                );

        handler.handle(message);
    }
}
