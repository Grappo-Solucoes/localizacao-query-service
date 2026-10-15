package com.busco.localizacao.app;

import reactor.core.publisher.Mono;

/**
 * Porta de saida da posicao projetada, para o acompanhamento em tempo real.
 *
 * <p>O dominio declara o contrato e a tecnologia (pub/sub do Redis) fica em {@code infra/redis}.</p>
 */
public interface PosicaoPublisher {

    Mono<Void> publish(String viagemId, Object payload);
}
