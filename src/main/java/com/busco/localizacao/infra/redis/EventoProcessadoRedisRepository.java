package com.busco.localizacao.infra.redis;

import com.busco.localizacao.domain.repository.EventoProcessadoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Marcador de idempotencia em Redis.
 *
 * <p>A janela e maior que a validade do read model: uma reentrega depois de o read model expirar
 * ainda precisa ser reconhecida como repeticao.</p>
 */
@Repository
public class EventoProcessadoRedisRepository implements EventoProcessadoRepository {

    private static final String PREFIXO = "evento:processado:";

    private final ReactiveStringRedisTemplate redis;
    private final Duration ttl;

    public EventoProcessadoRedisRepository(ReactiveStringRedisTemplate redis,
                                          @Value("${tracking.idempotencia.ttl-minutos:10}") long ttlMinutos) {
        this.redis = redis;
        this.ttl = Duration.ofMinutes(ttlMinutos);
    }

    @Override
    public Mono<Boolean> registrarSeNovo(String eventId) {
        return redis.opsForValue().setIfAbsent(PREFIXO + eventId, "1", ttl);
    }

    @Override
    public Mono<Boolean> liberar(String eventId) {
        return redis.opsForValue().delete(PREFIXO + eventId);
    }
}
