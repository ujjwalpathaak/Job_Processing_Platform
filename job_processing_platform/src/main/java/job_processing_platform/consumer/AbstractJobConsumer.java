package job_processing_platform.consumer;

import com.rabbitmq.client.Channel;
import job_processing_platform.dto.JobMessage;
import job_processing_platform.interfaces.Consumer;
import job_processing_platform.interfaces.JobHandler;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.util.List;

public abstract class AbstractJobConsumer implements Consumer<JobMessage> {

    protected final List<JobHandler> handlers;

    protected AbstractJobConsumer(List<JobHandler> handlers) {
        this.handlers = handlers;
    }

    protected void process(JobMessage message, Channel channel, Message raw) throws IOException {
        long tag = raw.getMessageProperties().getDeliveryTag();

        JobHandler handler = handlers.stream()
                .filter(h -> h.definition().identify().equals(message.getJobType()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No JobHandler found for type: " + message.getJobType()
                        )
                );

        try {
            handler.handle(message);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            // retry / DLQ logic handled by broker bindings
            channel.basicNack(tag, false, false);
            throw e;
        }
    }
}