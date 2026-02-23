package com.busco.localizacao.ui.rest;

import com.busco.localizacao.websocket.RoomSocketHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/metrics")
public class WebSocketMetricsController {

    private final RoomSocketHandler handler;

    public WebSocketMetricsController(RoomSocketHandler handler) {
        this.handler = handler;
    }

    @GetMapping("/connections")
    public String connections() {
        return "busco_websocket_connections " +
                handler.getConnections();
    }
}