package com.busco.localizacao.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.*;
import reactor.core.publisher.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoomSocketHandler implements WebSocketHandler {

    private final Map<String, Sinks.Many<String>> rooms =
            new ConcurrentHashMap<>();

    private final AtomicInteger connections = new AtomicInteger();


    @Override
    public Mono<Void> handle(WebSocketSession session) {
        connections.incrementAndGet();

        String path = session.getHandshakeInfo()
                .getUri()
                .getPath();

        String viagemId = path.split("/")[3];

        Sinks.Many<String> sink =
                rooms.computeIfAbsent(
                        viagemId,
                        k -> Sinks.many().multicast().onBackpressureBuffer()
                );

        Flux<WebSocketMessage> output =
                sink.asFlux()
                        .map(session::textMessage);

        return session.send(output);
    }

    public Mono<Void> publish(
            String viagemId,
            String json
    ) {

        Sinks.Many<String> sink = rooms.get(viagemId);

        if (sink != null) {
            sink.tryEmitNext(json);
        }

        return Mono.empty();
    }

    public Mono<Void> broadcast(
            String viagemId,
            String json
    ) {

        Sinks.Many<String> sink = rooms.get(viagemId);

        if (sink != null) {
            sink.tryEmitNext(json);
        }

        return Mono.empty();
    }

    public AtomicInteger getConnections() {
        return connections;
    }
}