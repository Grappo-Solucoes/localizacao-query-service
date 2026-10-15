package com.busco.localizacao.support;

import com.busco.localizacao.domain.repository.EventoProcessadoRepository;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Marcador de idempotencia em memoria para os testes de unidade, com a mesma semantica do
 * adaptador de Redis: registra na primeira entrega e libera quando o processamento falha.
 */
public class EventoProcessadoEmMemoria implements EventoProcessadoRepository {

    private final Set<String> registrados = Collections.synchronizedSet(new HashSet<>());

    @Override
    public Mono<Boolean> registrarSeNovo(String eventId) {
        return Mono.just(registrados.add(eventId));
    }

    @Override
    public Mono<Boolean> liberar(String eventId) {
        return Mono.just(registrados.remove(eventId));
    }
}
