package job_processing_platform.config;

import job_processing_platform.enums.JobCategory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    private final RabbitProperties props;

    @Value("${job.platform.rabbit.log-rag.queue:log.rag.queue}")
    private String logRagQueueName;

    public RabbitConfig(RabbitProperties props) {
        this.props = props;
    }

    @Bean
    public Queue logRagQueue() {
        return QueueBuilder.durable(logRagQueueName).build();
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    private SimpleRabbitListenerContainerFactory createBaseFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory criticalFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory f =
                createBaseFactory(connectionFactory, messageConverter);

        f.setConcurrentConsumers(1);
        f.setMaxConcurrentConsumers(3);
        f.setPrefetchCount(1);

        return f;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory standardFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory f =
                createBaseFactory(connectionFactory, messageConverter);

        f.setConcurrentConsumers(10);
        f.setMaxConcurrentConsumers(20);
        f.setPrefetchCount(5);

        return f;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory externalFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory f =
                createBaseFactory(connectionFactory, messageConverter);

        f.setConcurrentConsumers(2);
        f.setMaxConcurrentConsumers(5);
        f.setPrefetchCount(1);

        return f;
    }

    @Bean
    public DirectExchange standardExchange() {
        return new DirectExchange(
                props.getRabbit().getExchanges().get(JobCategory.STANDARD)
        );
    }

    @Bean
    public DirectExchange criticalExchange() {
        return new DirectExchange(
                props.getRabbit().getExchanges().get(JobCategory.CRITICAL)
        );
    }

    @Bean
    public DirectExchange externalExchange() {
        return new DirectExchange(
                props.getRabbit().getExchanges().get(JobCategory.EXTERNAL)
        );
    }

    @Bean
    public Queue standardQueue() {
        return mainQueue(
                props.getRabbit().getStandard(),
                props.getRabbit().getExchanges().get(JobCategory.STANDARD)
        );
    }

    @Bean
    public Queue criticalQueue() {
        return mainQueue(
                props.getRabbit().getCritical(),
                props.getRabbit().getExchanges().get(JobCategory.CRITICAL)
        );
    }

    @Bean
    public Queue externalQueue() {
        return mainQueue(
                props.getRabbit().getExternal(),
                props.getRabbit().getExchanges().get(JobCategory.EXTERNAL)
        );
    }

    private Queue mainQueue(RabbitProperties.Queue cfg, String exchange) {
        return QueueBuilder
                .durable(cfg.getQueue())
                .withArgument("x-dead-letter-exchange", exchange)
                // consumer decides retry routing key dynamically
                .build();
    }

    @Bean
    public Declarables retryQueues() {
        Declarables declarables = new Declarables();

        Queue retryRouteQueue = QueueBuilder
                .durable("retry.route.queue")
                .build();

        declarables.getDeclarables().add(retryRouteQueue);

        props.getRabbit().getRetries().forEach((key, retry) -> {

            Queue delayQueue = QueueBuilder
                    .durable(retry.getQueue())
                    .withArgument("x-message-ttl", retry.getTtl())
                    .withArgument("x-dead-letter-exchange", "")
                    .withArgument(
                            "x-dead-letter-routing-key",
                            retryRouteQueue.getName()
                    )
                    .build();

            declarables.getDeclarables().add(delayQueue);
        });

        return declarables;
    }

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

    @Bean
    public Declarables bindings() {
        return new Declarables(
                BindingBuilder.bind(standardQueue())
                        .to(standardExchange())
                        .with(props.getRabbit().getStandard().getRoutingKey()),
                BindingBuilder.bind(standardDlq())
                        .to(standardExchange())
                        .with(props.getRabbit().getStandard().getDlqRoutingKey()),
                BindingBuilder.bind(criticalQueue())
                        .to(criticalExchange())
                        .with(props.getRabbit().getCritical().getRoutingKey()),
                BindingBuilder.bind(criticalDlq())
                        .to(criticalExchange())
                        .with(props.getRabbit().getCritical().getDlqRoutingKey()),
                BindingBuilder.bind(externalQueue())
                        .to(externalExchange())
                        .with(props.getRabbit().getExternal().getRoutingKey()),
                BindingBuilder.bind(externalDlq())
                        .to(externalExchange())
                        .with(props.getRabbit().getExternal().getDlqRoutingKey())
        );
    }
}