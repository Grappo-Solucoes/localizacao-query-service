package com.busco.localizacao.websocket;

import com.busco.localizacao.domain.ViagemTrackingView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RoomPublisher {

    private final RoomSocketHandler handler;
    private final ObjectMapper mapper = new ObjectMapper();

    public RoomPublisher(RoomSocketHandler handler) {
        this.handler = handler;
    }

    public Mono<Void> publish(
            String viagemId,
            ViagemTrackingView view
    ) {

        try {

            String json = mapper.writeValueAsString(view);

            return handler.publish(viagemId, json);

        } catch (Exception e) {
            return Mono.empty();
        }
    }
}