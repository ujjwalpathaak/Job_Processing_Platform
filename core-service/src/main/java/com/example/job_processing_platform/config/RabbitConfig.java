package com.example.job_processing_platform.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    private final RabbitProperties props;

    public RabbitConfig(RabbitProperties props) {
        this.props = props;
    }

    // ================= COMMON =================

    @Bean
    public Jackson2JsonMessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter
    ) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

    // ================= EXCHANGES =================

    @Bean
    public DirectExchange jobExchange() {
        return new DirectExchange(props.getRabbit().getExchange());
    }

    @Bean
    public DirectExchange retryExchange() {
        return new DirectExchange(props.getRabbit().getRetryExchange());
    }

    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(props.getRabbit().getDlqExchange());
    }

    // ================= FAST =================

    @Bean
    public Queue fastQueue() {
        return QueueBuilder.durable(props.getRabbit().getFast().getQueue())
                .withArgument("x-dead-letter-exchange", props.getRabbit().getRetryExchange())
                .withArgument("x-dead-letter-routing-key",
                        props.getRabbit().getFast().getRetryRoutingKey())
                .build();
    }

    @Bean
    public Queue fastRetryQueue() {
        return QueueBuilder.durable(props.getRabbit().getFast().getRetryQueue())
                .withArgument("x-message-ttl", props.getRabbit().getFast().getRetryTtl())
                .withArgument("x-dead-letter-exchange", props.getRabbit().getExchange())
                .withArgument("x-dead-letter-routing-key",
                        props.getRabbit().getFast().getRoutingKey())
                .build();
    }

    @Bean
    public Queue fastDlq() {
        return QueueBuilder.durable(props.getRabbit().getFast().getDlq()).build();
    }

    @Bean
    public Binding fastBinding() {
        return BindingBuilder.bind(fastQueue())
                .to(jobExchange())
                .with(props.getRabbit().getFast().getRoutingKey());
    }

    @Bean
    public Binding fastRetryBinding() {
        return BindingBuilder.bind(fastRetryQueue())
                .to(retryExchange())
                .with(props.getRabbit().getFast().getRetryRoutingKey());
    }

    @Bean
    public Binding fastDlqBinding() {
        return BindingBuilder.bind(fastDlq())
                .to(dlqExchange())
                .with(props.getRabbit().getFast().getDlqRoutingKey());
    }

    // ================= SLOW =================

    @Bean
    public Queue slowQueue() {
        return QueueBuilder.durable(props.getRabbit().getSlow().getQueue())
                .withArgument("x-dead-letter-exchange", props.getRabbit().getRetryExchange())
                .withArgument("x-dead-letter-routing-key",
                        props.getRabbit().getSlow().getRetryRoutingKey())
                .build();
    }

    @Bean
    public Queue slowRetryQueue() {
        return QueueBuilder.durable(props.getRabbit().getSlow().getRetryQueue())
                .withArgument("x-message-ttl", props.getRabbit().getSlow().getRetryTtl())
                .withArgument("x-dead-letter-exchange", props.getRabbit().getExchange())
                .withArgument("x-dead-letter-routing-key",
                        props.getRabbit().getSlow().getRoutingKey())
                .build();
    }

    @Bean
    public Queue slowDlq() {
        return QueueBuilder.durable(props.getRabbit().getSlow().getDlq()).build();
    }

    @Bean
    public Binding slowBinding() {
        return BindingBuilder.bind(slowQueue())
                .to(jobExchange())
                .with(props.getRabbit().getSlow().getRoutingKey());
    }

    @Bean
    public Binding slowRetryBinding() {
        return BindingBuilder.bind(slowRetryQueue())
                .to(retryExchange())
                .with(props.getRabbit().getSlow().getRetryRoutingKey());
    }

    @Bean
    public Binding slowDlqBinding() {
        return BindingBuilder.bind(slowDlq())
                .to(dlqExchange())
                .with(props.getRabbit().getSlow().getDlqRoutingKey());
    }

    // ================= CRITICAL =================

    @Bean
    public Queue criticalQueue() {
        return QueueBuilder.durable(props.getRabbit().getCritical().getQueue())
                .withArgument("x-dead-letter-exchange", props.getRabbit().getRetryExchange())
                .withArgument("x-dead-letter-routing-key",
                        props.getRabbit().getCritical().getRetryRoutingKey())
                .build();
    }

    @Bean
    public Queue criticalRetryQueue() {
        return QueueBuilder.durable(props.getRabbit().getCritical().getRetryQueue())
                .withArgument("x-message-ttl", props.getRabbit().getCritical().getRetryTtl())
                .withArgument("x-dead-letter-exchange", props.getRabbit().getExchange())
                .withArgument("x-dead-letter-routing-key",
                        props.getRabbit().getCritical().getRoutingKey())
                .build();
    }

    @Bean
    public Queue criticalDlq() {
        return QueueBuilder.durable(props.getRabbit().getCritical().getDlq()).build();
    }

    @Bean
    public Binding criticalBinding() {
        return BindingBuilder.bind(criticalQueue())
                .to(jobExchange())
                .with(props.getRabbit().getCritical().getRoutingKey());
    }

    @Bean
    public Binding criticalRetryBinding() {
        return BindingBuilder.bind(criticalRetryQueue())
                .to(retryExchange())
                .with(props.getRabbit().getCritical().getRetryRoutingKey());
    }

    @Bean
    public Binding criticalDlqBinding() {
        return BindingBuilder.bind(criticalDlq())
                .to(dlqExchange())
                .with(props.getRabbit().getCritical().getDlqRoutingKey());
    }

    // ================= LOGGING =================

    @Bean
    public Queue logQueue() {
        return QueueBuilder.durable(props.getRabbit().getLogQueue()).build();
    }

    @Bean
    public Binding logBinding() {
        return BindingBuilder.bind(logQueue())
                .to(jobExchange())
                .with(props.getRabbit().getLogRoutingKey());
    }
}