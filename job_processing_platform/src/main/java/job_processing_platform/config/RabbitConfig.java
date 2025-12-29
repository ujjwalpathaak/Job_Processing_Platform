package job_processing_platform.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    private final RabbitProperties props;

    public RabbitConfig(RabbitProperties props) {
        this.props = props;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        return factory;
    }

    // ------------------------------------------------
    // Exchange
    // ------------------------------------------------
    @Bean
    public DirectExchange standardExchange() {
        return new DirectExchange(props.getRabbit().getStandardExchange());
    }

    // ------------------------------------------------
    // Main Queue
    // ------------------------------------------------
    @Bean
    public Queue standardQueue() {
        return QueueBuilder
                .durable(props.getRabbit().getStandard().getQueue())
                .build();
    }

    // ------------------------------------------------
    // Retry Queue - 30s
    // ------------------------------------------------
    @Bean
    public Queue standardRetry30sQueue() {
        return QueueBuilder
                .durable(props.getRabbit().getStandard().getRetry30s().getQueue())
                .withArgument("x-message-ttl",
                        props.getRabbit().getStandard().getRetry30s().getTtl())
                .withArgument("x-dead-letter-exchange",
                        props.getRabbit().getStandardExchange())
                .withArgument("x-dead-letter-routing-key",
                        props.getRabbit().getStandard().getRoutingKey())
                .build();
    }

    // ------------------------------------------------
    // DLQ
    // ------------------------------------------------
    @Bean
    public Queue standardDlq() {
        return QueueBuilder
                .durable(props.getRabbit().getStandard().getDlq())
                .build();
    }

    // ------------------------------------------------
    // Bindings
    // ------------------------------------------------
    @Bean
    public Binding standardBinding() {
        return BindingBuilder
                .bind(standardQueue())
                .to(standardExchange())
                .with(props.getRabbit().getStandard().getRoutingKey());
    }

    @Bean
    public Binding standardRetry30sBinding() {
        return BindingBuilder
                .bind(standardRetry30sQueue())
                .to(standardExchange())
                .with(props.getRabbit().getStandard().getRetry30s().getRoutingKey());
    }

    @Bean
    public Binding standardDlqBinding() {
        return BindingBuilder
                .bind(standardDlq())
                .to(standardExchange())
                .with(props.getRabbit().getStandard().getDlqRoutingKey());
    }
}