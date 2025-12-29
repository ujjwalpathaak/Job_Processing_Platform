package job_processing_platform.interfaces;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

import java.io.IOException;

public interface Consumer<T> {
    void consume(T message, Channel channel, Message amqpMessage) throws IOException;
}