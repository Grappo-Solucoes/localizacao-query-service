package com.busco.localizacao.ui.rest;

import com.busco.localizacao.websocket.SalasDeAcompanhamento;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/metrics")
public class WebSocketMetricsController {

    private final SalasDeAcompanhamento salas;

    public WebSocketMetricsController(SalasDeAcompanhamento salas) {
        this.salas = salas;
    }

    @GetMapping("/connections")
    public String connections() {
        return "busco_websocket_connections " +
                salas.conexoes();
    }
}
