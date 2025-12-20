package com.example.job_processing_platform.workerservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;

@Configuration
public class ConsumerRabbitConfig {

    public static final String JOB_QUEUE = "job.queue";
    public static final String JOB_EXCHANGE = "job.exchange";
    public static final String JOB_ROUTING_KEY = "job.routing.key";

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
        return QueueBuilder.durable(JOB_QUEUE).build();
    }

    @Bean
    public DirectExchange consumerJobExchange() {
        return new DirectExchange(JOB_EXCHANGE);
    }

    @Bean
    public Binding consumerJobBinding() {
        return BindingBuilder
                .bind(consumerJobQueue())
                .to(consumerJobExchange())
                .with(JOB_ROUTING_KEY);
    }
}