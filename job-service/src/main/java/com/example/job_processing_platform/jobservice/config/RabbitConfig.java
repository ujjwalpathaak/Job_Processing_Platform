package com.example.job_processing_platform.jobservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    private static String JOB_EXCHANGE;
    private static String JOB_QUEUE;
    private static String JOB_ROUTING_KEY;

    public RabbitConfig(JobPlatformProperties properties){
        RabbitConfig.JOB_QUEUE = properties.getRabbit().getQueue();
        RabbitConfig.JOB_ROUTING_KEY = properties.getRabbit().getExchange();
        RabbitConfig.JOB_EXCHANGE = properties.getRabbit().getRoutingKey();
    }

    public static String getJobExchange() {
        return JOB_EXCHANGE;
    }

    public static String getJobQueue() {
        return JOB_QUEUE;
    }

    public static String getJobRoutingKey() {
        return JOB_ROUTING_KEY;
    }

    @Bean
    public DirectExchange jobExchange() {
        return new DirectExchange(RabbitConfig.JOB_EXCHANGE);
    }

    @Bean
    public Jackson2JsonMessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            @Qualifier("jacksonMessageConverter")
            Jackson2JsonMessageConverter jacksonMessageConverter
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public Queue jobQueue() {
        return QueueBuilder.durable(RabbitConfig.JOB_QUEUE).build();
    }

    @Bean
    public Binding jobBinding() {
        return BindingBuilder
                .bind(jobQueue())
                .to(jobExchange())
                .with(RabbitConfig.JOB_ROUTING_KEY);
    }
}