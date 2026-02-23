package com.busco.localizacao_query_service.infra.redis;

import com.busco.localizacao_query_service.domain.AlunoTrackingView;
import com.busco.localizacao_query_service.domain.ViagemTrackingView;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Repository
public class AlunoTrackingRedisRepository {

    private static final Duration TTL = Duration.ofSeconds(30);

    private final ReactiveRedisTemplate<String, AlunoTrackingView> redis;

    public AlunoTrackingRedisRepository(
            @Qualifier("alunoTrackingRedisTemplate")
            ReactiveRedisTemplate<String, AlunoTrackingView> redis
    ) {
        this.redis = redis;
    }

    public Mono<Boolean> save(AlunoTrackingView view) {

        String key = key(view.getAlunoId());

        return redis
                .opsForValue()
                .set(key, view, TTL);
    }

    public Mono<AlunoTrackingView> find(String alunoId) {
        return redis.opsForValue().get(key(alunoId));
    }

    private String key(String alunoId) {
        return "tracking:aluno:" + alunoId;
    }
}