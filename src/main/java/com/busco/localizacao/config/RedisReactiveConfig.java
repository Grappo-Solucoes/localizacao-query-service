package com.busco.localizacao.config;


import com.busco.localizacao.domain.AlunoTrackingView;
import com.busco.localizacao.domain.ViagemTrackingView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

@Configuration
public class RedisReactiveConfig {

    private <T> ReactiveRedisTemplate<String, T> buildTemplate(
            ReactiveRedisConnectionFactory factory,
            ObjectMapper mapper,
            Class<T> clazz
    ) {

        Jackson2JsonRedisSerializer<T> serializer =
                new Jackson2JsonRedisSerializer<>(mapper, clazz);

        RedisSerializationContext.RedisSerializationContextBuilder<String, T> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, T> context =
                builder
                        .value(serializer)
                        .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, ViagemTrackingView> viagemTrackingRedisTemplate(
            ReactiveRedisConnectionFactory factory,
            ObjectMapper mapper
    ) {
        return buildTemplate(factory, mapper, ViagemTrackingView.class);
    }

    @Bean
    public ReactiveRedisTemplate<String, AlunoTrackingView> alunoTrackingRedisTemplate(
            ReactiveRedisConnectionFactory factory,
            ObjectMapper mapper
    ) {
        return buildTemplate(factory, mapper, AlunoTrackingView.class);
    }
}