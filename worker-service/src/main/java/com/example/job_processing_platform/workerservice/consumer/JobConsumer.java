package com.example.job_processing_platform.workerservice.consumer;

import com.example.job_processing_platform.dto.JobMessage;
import com.example.job_processing_platform.interfaces.Consumer;
import com.example.job_processing_platform.interfaces.JobHandler;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class JobConsumer implements Consumer<JobMessage> {

    private final List<JobHandler> handlers;

    public JobConsumer(List<JobHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    @RabbitListener(
            queues = "${job.platform.rabbit.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consume(JobMessage message, Channel channel, Message raw) throws IOException {
//        long tag = raw.getMessageProperties().getDeliveryTag();
//        channel.basicAck(tag, true);
//        channel.basicNack(deliveryTag, false, true);
//        channel.basicReject(tag, false);

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