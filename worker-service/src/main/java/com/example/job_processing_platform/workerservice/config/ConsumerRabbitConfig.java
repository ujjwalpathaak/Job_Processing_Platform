package com.example.job_processing_platform.workerservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsumerRabbitConfig {

    private final ConsumerJobPlatformProperties properties;

    public ConsumerRabbitConfig(ConsumerJobPlatformProperties properties) {
        this.properties = properties;
    }

    @Bean
    public Jackson2JsonMessageConverter consumerJacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            @Qualifier("consumerJacksonMessageConverter")
            Jackson2JsonMessageConverter converter
    ) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }

    @Bean
    public Queue consumerJobQueue() {
        return QueueBuilder
                .durable(properties.getRabbit().getQueue())
                .build();
    }

    @Bean
    public DirectExchange consumerJobExchange() {
        return new DirectExchange(properties.getRabbit().getExchange());
    }

    @Bean
    public Binding consumerJobBinding() {
        return BindingBuilder
                .bind(consumerJobQueue())
                .to(consumerJobExchange())
                .with(properties.getRabbit().getRoutingKey());
    }

    @Bean
    public Queue consumerLogQueue() {
        return QueueBuilder
                .durable(properties.getRabbit().getLogQueue())
                .build();
    }

    @Bean
    public Binding consumerLogBinding() {
        return BindingBuilder
                .bind(consumerLogQueue())
                .to(consumerJobExchange())
                .with(properties.getRabbit().getLogRoutingKey());
    }
}