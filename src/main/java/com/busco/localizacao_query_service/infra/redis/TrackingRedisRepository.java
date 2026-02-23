package com.busco.localizacao_query_service.infra.redis;

import com.busco.localizacao_query_service.domain.ViagemTrackingView;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Repository
public class TrackingRedisRepository {

    private static final Duration TTL = Duration.ofSeconds(10);

    private final ReactiveRedisTemplate<String, ViagemTrackingView> redis;

    public TrackingRedisRepository(
            @Qualifier("viagemTrackingRedisTemplate")
            ReactiveRedisTemplate<String, ViagemTrackingView> redis
    ) {
        this.redis = redis;
    }

    public Mono<Boolean> save(ViagemTrackingView view) {

        String key = key(view.getViagemId());

        return redis
                .opsForValue()
                .set(key, view, TTL);
    }

    public Mono<ViagemTrackingView> find(String viagemId) {
        return redis.opsForValue().get(key(viagemId));
    }

    private String key(String viagemId) {
        return "tracking:viagem:" + viagemId;
    }
}