package com.busco.localizacao.infra.redis;

import org.junit.jupiter.api.Test;
import org.springframework.util.PatternMatchUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contrato do canal de tempo real: quem publica e quem assina precisam concordar. */
class CanalDeViagemTest {

    @Test
    void oIdentificadorVoltaDoNomeDoCanal() {
        assertEquals("viagem-42", CanalDeViagem.viagemId(CanalDeViagem.nome("viagem-42")));
    }

    @Test
    void oPadraoDeAssinaturaCobreONomeDoCanal() {
        assertTrue(PatternMatchUtils.simpleMatch(CanalDeViagem.PADRAO, CanalDeViagem.nome("viagem-42")),
                "o padrao assinado deveria casar com o canal publicado: " + CanalDeViagem.nome("viagem-42"));
    }

    @Test
    void canalDeOutroAssuntoNaoIdentificaViagem() {
        assertNull(CanalDeViagem.viagemId("outro.canal"));
        assertNull(CanalDeViagem.viagemId(null));
    }
}
