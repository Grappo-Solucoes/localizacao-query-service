package com.busco.localizacao_query_service.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "location.exchange";
    public static final String QUEUE = "location.query.queue";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue queue() {
        return QueueBuilder
                .durable(QUEUE)
                .build();
    }

    @Bean
    public Binding binding(
            Queue queue,
            TopicExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with("viagem.posicao");
    }
}