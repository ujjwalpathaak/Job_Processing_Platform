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

    // =================================================
    // COMMON
    // =================================================
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

    // =================================================
    // EXCHANGES
    // =================================================
    @Bean
    public DirectExchange standardExchange() {
        return new DirectExchange(
                props.getRabbit().getExchanges().get("standard")
        );
    }

    @Bean
    public DirectExchange criticalExchange() {
        return new DirectExchange(
                props.getRabbit().getExchanges().get("critical")
        );
    }

    @Bean
    public DirectExchange externalExchange() {
        return new DirectExchange(
                props.getRabbit().getExchanges().get("external")
        );
    }

    // =================================================
    // MAIN QUEUES
    // =================================================
    @Bean
    public Queue standardQueue() {
        return mainQueue(
                props.getRabbit().getStandard(),
                props.getRabbit().getExchanges().get("standard")
        );
    }

    @Bean
    public Queue criticalQueue() {
        return mainQueue(
                props.getRabbit().getCritical(),
                props.getRabbit().getExchanges().get("critical")
        );
    }

    @Bean
    public Queue externalQueue() {
        return mainQueue(
                props.getRabbit().getExternal(),
                props.getRabbit().getExchanges().get("external")
        );
    }

    private Queue mainQueue(RabbitProperties.Queue cfg, String exchange) {
        return QueueBuilder
                .durable(cfg.getQueue())
                .withArgument("x-dead-letter-exchange", exchange)
                // consumer decides retry routing key dynamically
                .build();
    }

    // =================================================
    // SHARED RETRY QUEUES
    // =================================================
    @Bean
    public Declarables retryQueues() {
        Declarables declarables = new Declarables();

        props.getRabbit().getRetries().forEach((key, retry) -> {
            Queue queue = QueueBuilder
                    .durable(retry.getQueue())
                    .withArgument("x-message-ttl", retry.getTtl())
                    // route BACK to original exchange
                    .withArgument("x-dead-letter-exchange", "")
                    // routing key is set when publishing to retry
                    .build();

            declarables.getDeclarables().add(queue);
        });

        return declarables;
    }

    // =================================================
    // DLQs
    // =================================================
    @Bean
    public Queue standardDlq() {
        return QueueBuilder
                .durable(props.getRabbit().getStandard().getDlq())
                .build();
    }

    @Bean
    public Queue criticalDlq() {
        return QueueBuilder
                .durable(props.getRabbit().getCritical().getDlq())
                .build();
    }

    @Bean
    public Queue externalDlq() {
        return QueueBuilder
                .durable(props.getRabbit().getExternal().getDlq())
                .build();
    }

    // =================================================
    // BINDINGS
    // =================================================
    @Bean
    public Declarables bindings() {
        return new Declarables(
                // STANDARD
                BindingBuilder.bind(standardQueue())
                        .to(standardExchange())
                        .with(props.getRabbit().getStandard().getRoutingKey()),

                BindingBuilder.bind(standardDlq())
                        .to(standardExchange())
                        .with(props.getRabbit().getStandard().getDlqRoutingKey()),

                // CRITICAL
                BindingBuilder.bind(criticalQueue())
                        .to(criticalExchange())
                        .with(props.getRabbit().getCritical().getRoutingKey()),

                BindingBuilder.bind(criticalDlq())
                        .to(criticalExchange())
                        .with(props.getRabbit().getCritical().getDlqRoutingKey()),

                // EXTERNAL
                BindingBuilder.bind(externalQueue())
                        .to(externalExchange())
                        .with(props.getRabbit().getExternal().getRoutingKey()),

                BindingBuilder.bind(externalDlq())
                        .to(externalExchange())
                        .with(props.getRabbit().getExternal().getDlqRoutingKey())
        );
    }
}