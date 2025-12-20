package com.example.job_processing_platform.jobservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(JobPlatformProperties.class)
public class RabbitConfig {

    private final JobPlatformProperties properties;

    public RabbitConfig(JobPlatformProperties properties) {
        this.properties = properties;
    }

    // ----------- Infrastructure -----------

    @Bean
    public DirectExchange jobExchange() {
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
                .to(jobExchange())
                .with(properties.getRabbit().getRoutingKey());
    }

    // ----------- Serialization -----------

    @Bean
    public Jackson2JsonMessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ----------- Producer -----------

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            @Qualifier("jacksonMessageConverter")
            Jackson2JsonMessageConverter converter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}