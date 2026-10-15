package com.busco.localizacao.infra.redis;

import com.busco.localizacao.app.PosicaoPublisher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/** Publicador de evento em memoria: registra o que a projecao publicou, sem Redis. */
public class PublicadorDeEventoFake implements PosicaoPublisher {

    private final List<String> viagensPublicadas = new ArrayList<>();

    @Override
    public Mono<Void> publish(String viagemId, Object payload) {
        viagensPublicadas.add(viagemId);
        return Mono.empty();
    }

    public List<String> viagensPublicadas() {
        return List.copyOf(viagensPublicadas);
    }
}
