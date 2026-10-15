package com.busco.localizacao.websocket;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Salas de acompanhamento em tempo real: uma por viagem, com as conexoes que ouvem aquela viagem.
 *
 * <p>A sala nasce na assinatura e sai do mapa quando a ultima conexao encerra, para nao acumular
 * sala de viagem que ninguem mais acompanha.</p>
 */
@Component
public class SalasDeAcompanhamento {

    private final Map<String, Sala> salas = new ConcurrentHashMap<>();

    /**
     * Fluxo de uma conexao que acompanha a viagem: a sala e criada na assinatura e liberada quando
     * ela encerra.
     */
    public Flux<String> entrar(String viagemId) {
        return Flux.defer(() -> {
            Sala sala = salas.computeIfAbsent(viagemId, id -> new Sala());
            sala.conectar();

            return sala.fluxo().doFinally(sinal -> {
                sala.desconectar();
                salas.computeIfPresent(viagemId, (id, atual) -> atual.conexoes() == 0 ? null : atual);
            });
        });
    }

    /**
     * Entrega o payload as conexoes daquela viagem. Sem conexao na sala nada e guardado: quem nao
     * estava ouvindo perde a posicao.
     */
    public Mono<Void> publicar(String viagemId, String payload) {
        if (viagemId == null || viagemId.isBlank()) {
            return Mono.empty();
        }

        Sala sala = salas.get(viagemId);
        if (sala != null) {
            sala.emitir(payload);
        }
        return Mono.empty();
    }

    /** Conexoes abertas no momento, em todas as viagens. */
    public int conexoes() {
        return salas.values().stream().mapToInt(Sala::conexoes).sum();
    }

    /** Viagens com pelo menos uma conexao aberta. */
    public int salasAtivas() {
        return salas.size();
    }

    private static final class Sala {

        private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        private final AtomicInteger conexoes = new AtomicInteger();

        Flux<String> fluxo() {
            return sink.asFlux();
        }

        void conectar() {
            conexoes.incrementAndGet();
        }

        void desconectar() {
            conexoes.decrementAndGet();
        }

        int conexoes() {
            return conexoes.get();
        }

        void emitir(String payload) {
            sink.tryEmitNext(payload);
        }
    }
}
