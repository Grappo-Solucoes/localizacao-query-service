package com.busco.localizacao.domain.repository;

import reactor.core.publisher.Mono;

/**
 * Registro dos {@code eventId} ja processados, para tornar o consumo idempotente.
 *
 * <p>O consumo e at-least-once: rebalanceamento e reentrega podem repetir um evento. A
 * implementacao vive em {@code infra/redis}, com janela propria.</p>
 */
public interface EventoProcessadoRepository {

    /** {@code true} na primeira entrega do evento; {@code false} quando ele ja havia sido registrado. */
    Mono<Boolean> registrarSeNovo(String eventId);

    /**
     * Libera o registro de um evento cujo processamento falhou, para que a reentrega volte a
     * processa-lo em vez de descarta-lo como duplicata.
     */
    Mono<Boolean> liberar(String eventId);
}
