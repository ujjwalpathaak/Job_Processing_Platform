package job_processing_platform.consumer;

import com.rabbitmq.client.Channel;
import job_processing_platform.dto.JobMessage;
import job_processing_platform.interfaces.JobHandler;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class StandardQueueConsumer extends AbstractJobConsumer {

    public StandardQueueConsumer(List<JobHandler> handlers) {
        super(handlers);
    }

    @Override
    @RabbitListener(
            queues = "${job.platform.rabbit.standard.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consume(JobMessage message, Channel channel, Message raw) throws IOException {
        long tag = raw.getMessageProperties().getDeliveryTag();
        JobHandler handler = this.getHandler(message.getJobType());

        try {
            handler.handle(message);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            // retry / DLQ logic handled by broker bindings
            channel.basicNack(tag, false, false);
            throw e;
        }

        try {
            handler.handle(message);
        } catch (Exception ignored) {

        }
        // check karo run tho nhi hogya hai phele se
        // agar hogya hai tho update logs?
        // try
        // agar hogya pass
        // logs update kardo
        // catch
        // issue aya
        // tho ab isko retry queue mei daldo
    }

    private JobHandler getHandler(String jobType) throws IOException {
        return handlers.stream()
                .filter(h -> h.definition().identify().equals(jobType))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No JobHandler found for type: " + jobType
                        )
                );
    }
}