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

    private final Properties properties;

    public RabbitConfig(Properties properties) {
        this.properties = properties;
    }

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
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(properties.getRabbit().getExchange());
    }

    @Bean
    public Queue jobQueue() {
        return QueueBuilder
                .durable(properties.getRabbit().getQueue())
                .build();
    }

    @Bean
    public Binding jobBinding() {
        return BindingBuilder
                .bind(jobQueue())
                .to(exchange())
                .with(properties.getRabbit().getRoutingKey());
    }

    @Bean
    public Queue logQueue() {
        return QueueBuilder
                .durable(properties.getRabbit().getLogQueue())
                .build();
    }

    @Bean
    public Binding logBinding() {
        return BindingBuilder
                .bind(logQueue())
                .to(exchange())
                .with(properties.getRabbit().getLogRoutingKey());
    }
}