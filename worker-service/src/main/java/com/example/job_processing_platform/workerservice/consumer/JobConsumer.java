package com.example.job_processing_platform.workerservice.consumer;

import com.example.job_processing_platform.workerservice.dto.ConsumerJobMessage;
import com.example.job_processing_platform.workerservice.interfaces.JobHandler;
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
    public void consume(ConsumerJobMessage message) {

        JobHandler handler = handlers.stream()
                .filter(h -> h.getJobType().equals(message.getType()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No JobHandler found for type: " + message.getType()
                        )
                );

        handler.handle(message);
    }

    private void processEmailJob(ConsumerJobMessage message) {
        System.out.println("Processing EMAIL job: " + message.getId());
    }

    private void processPdfJob(ConsumerJobMessage message) {
        System.out.println("Processing PDF job: " + message.getId());
    }
}
