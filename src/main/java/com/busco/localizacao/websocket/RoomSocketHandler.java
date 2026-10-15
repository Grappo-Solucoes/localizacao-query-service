package com.busco.localizacao.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

/**
 * Acompanhamento de uma viagem por WebSocket: o caminho {@code /ws/viagem/{viagemId}} define a sala.
 *
 * <p>Handshake fora do padrao e recusado. As conexoes recebem o payload publicado na viagem
 * enquanto a sessao estiver aberta.</p>
 */
@Component
public class RoomSocketHandler implements WebSocketHandler {

    private final SalasDeAcompanhamento salas;

    public RoomSocketHandler(SalasDeAcompanhamento salas) {
        this.salas = salas;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String viagemId = CaminhosDaViagem.viagemId(session.getHandshakeInfo().getUri().getPath());
        if (viagemId == null) {
            return session.close(CloseStatus.BAD_DATA.withReason("caminho esperado: /ws/viagem/{viagemId}"));
        }

        return session.send(salas.entrar(viagemId).map(session::textMessage));
    }
}
