package com.busco.localizacao.ui.rest;

import com.busco.localizacao.domain.AlunoTrackingView;
import com.busco.localizacao.domain.ViagemTrackingView;
import com.busco.localizacao.support.PosicaoReadModelEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Consulta da ultima posicao projetada. */
class PosicaoQueryControllerTest {

    private static final long UMA_HORA = 3_600_000L;

    private PosicaoReadModelEmMemoria readModel;
    private PosicaoQueryController controller;

    @BeforeEach
    void montar() {
        readModel = new PosicaoReadModelEmMemoria();
        controller = new PosicaoQueryController(readModel);
    }

    @Test
    void devolveAPosicaoDaViagem() {
        readModel.salvarViagem(viewDeViagem(validaPor(UMA_HORA))).block();

        ResponseEntity<ViagemTrackingView> resposta = controller.posicaoDaViagem("viagem-42").block();

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(-23.5505, resposta.getBody().getLatitude());
        assertEquals(-46.6333, resposta.getBody().getLongitude());
        assertEquals(1_700_000_000_000L, resposta.getBody().getTimestamp());
    }

    @Test
    void sinalizaAusenciaQuandoNaoHaPosicaoDaViagem() {
        ResponseEntity<ViagemTrackingView> resposta = controller.posicaoDaViagem("viagem-inexistente").block();

        assertEquals(HttpStatus.NOT_FOUND, resposta.getStatusCode());
    }

    @Test
    void sinalizaAusenciaQuandoAPosicaoDaViagemExpirou() {
        readModel.salvarViagem(viewDeViagem(validaPor(-1))).block();

        ResponseEntity<ViagemTrackingView> resposta = controller.posicaoDaViagem("viagem-42").block();

        assertEquals(HttpStatus.NOT_FOUND, resposta.getStatusCode());
    }

    @Test
    void devolveAPosicaoDoAlunoComAViagemAssociada() {
        readModel.salvarAluno(new AlunoTrackingView("aluno-7", "viagem-42", -23.5505, -46.6333,
                1_700_000_000_000L, "6gyf4bf8", validaPor(UMA_HORA))).block();

        ResponseEntity<AlunoTrackingView> resposta = controller.posicaoDoAluno("aluno-7").block();

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals("viagem-42", resposta.getBody().getViagemId());
        assertEquals(-23.5505, resposta.getBody().getLatitude());
        assertEquals(1_700_000_000_000L, resposta.getBody().getTimestamp());
    }

    @Test
    void sinalizaAusenciaQuandoNaoHaPosicaoDoAluno() {
        ResponseEntity<AlunoTrackingView> resposta = controller.posicaoDoAluno("aluno-inexistente").block();

        assertEquals(HttpStatus.NOT_FOUND, resposta.getStatusCode());
    }

    @Test
    void sinalizaAusenciaQuandoAPosicaoDoAlunoExpirou() {
        readModel.salvarAluno(new AlunoTrackingView("aluno-7", "viagem-42", -23.5505, -46.6333,
                1_700_000_000_000L, "6gyf4bf8", validaPor(-1))).block();

        ResponseEntity<AlunoTrackingView> resposta = controller.posicaoDoAluno("aluno-7").block();

        assertEquals(HttpStatus.NOT_FOUND, resposta.getStatusCode());
    }

    private static ViagemTrackingView viewDeViagem(long expiraEm) {
        return new ViagemTrackingView("viagem-42", -23.5505, -46.6333, 1_700_000_000_000L,
                "6gyf4bf8", expiraEm, 88.5, 12.5, "gps");
    }

    private static long validaPor(long deslocamentoMillis) {
        return System.currentTimeMillis() + deslocamentoMillis;
    }
}
