package com.busco.localizacao.websocket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Extração do identificador da viagem a partir do caminho do handshake. */
class CaminhosDaViagemTest {

    @Test
    void extraiOIdentificadorDoCaminhoDaViagem() {
        assertEquals("viagem-42", CaminhosDaViagem.viagemId("/ws/viagem/viagem-42"));
    }

    @Test
    void recusaCaminhoSemOViagemId() {
        assertNull(CaminhosDaViagem.viagemId("/ws/viagem/"));
        assertNull(CaminhosDaViagem.viagemId("/ws/viagem"));
        assertNull(CaminhosDaViagem.viagemId("/ws/"));
        assertNull(CaminhosDaViagem.viagemId("/ws/viagem-42"));
    }

    @Test
    void recusaIdentificadorEmBranco() {
        assertNull(CaminhosDaViagem.viagemId("/ws/viagem/   "));
    }

    @Test
    void recusaCaminhoComSegmentoDepoisDoIdentificador() {
        assertNull(CaminhosDaViagem.viagemId("/ws/viagem/viagem-42/extra"));
    }
}
