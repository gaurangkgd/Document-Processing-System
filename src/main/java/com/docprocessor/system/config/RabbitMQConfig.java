package com.docprocessor.system.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PROCESSING_QUEUE = "document.processing.queue";
    public static final String DEAD_LETTER_QUEUE = "document.processing.dlq";
    public static final String PROCESSING_EXCHANGE = "document.processing.exchange";
    public static final String PROCESSING_ROUTING_KEY = "processing";

    @Bean
    public Queue processingQueue() {
        return new Queue(PROCESSING_QUEUE, true);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public DirectExchange processingExchange() {
        return new DirectExchange(PROCESSING_EXCHANGE);
    }

    @Bean
    public Binding processingBinding(Queue processingQueue, DirectExchange processingExchange) {
        return BindingBuilder.bind(processingQueue).to(processingExchange).with(PROCESSING_ROUTING_KEY);
    }
}
