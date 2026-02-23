package com.busco.localizacao_query_service.infra.redis;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Component
public class RedisPubSubPublisher {

    private final ReactiveRedisTemplate<String, String> redis;
    private final ObjectMapper mapper = new ObjectMapper();

    public RedisPubSubPublisher(
            ReactiveRedisTemplate<String, String> redis
    ) {
        this.redis = redis;
    }

    public Mono<Void> publish(
            String viagemId,
            Object payload
    ) {

        try {

            String json = mapper.writeValueAsString(payload);

            return redis
                    .convertAndSend(
                            "tracking.viagem." + viagemId,
                            json
                    )
                    .then();

        } catch (Exception e) {
            return Mono.empty();
        }
    }
}