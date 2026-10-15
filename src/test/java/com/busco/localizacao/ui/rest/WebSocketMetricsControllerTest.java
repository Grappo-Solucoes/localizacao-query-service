package com.busco.localizacao.ui.rest;

import com.busco.localizacao.websocket.SalasDeAcompanhamento;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Metrica de conexoes exposta para coleta. */
class WebSocketMetricsControllerTest {

    @Test
    void expoeAQuantidadeDeConexoesAbertas() {
        SalasDeAcompanhamento salas = new SalasDeAcompanhamento();
        WebSocketMetricsController controller = new WebSocketMetricsController(salas);

        assertEquals("busco_websocket_connections 0", controller.connections());

        Disposable primeira = salas.entrar("viagem-42").subscribe(ignorado -> { });
        Disposable segunda = salas.entrar("viagem-43").subscribe(ignorado -> { });

        assertEquals("busco_websocket_connections 2", controller.connections());

        primeira.dispose();

        assertEquals("busco_websocket_connections 1", controller.connections());
        segunda.dispose();
    }
}
