package com.busco.localizacao.config;

import com.busco.localizacao.websocket.RoomSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

import java.util.Map;

@Configuration
public class WebSocketConfig {

    @Bean
    public HandlerMapping webSocketMapping(
            RoomSocketHandler handler
    ) {

        return new SimpleUrlHandlerMapping(
                Map.of("/ws/viagem/**", handler),
                -1
        );
    }
}