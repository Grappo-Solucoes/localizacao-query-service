package com.busco.localizacao.infra.kafka;

import com.busco.localizacao.app.LocationProjectionService;
import com.busco.localizacao.app.geo.GeoHashService;
import com.busco.localizacao.domain.AlunoTrackingView;
import com.busco.localizacao.domain.events.PosicaoAlunoRegistrada;
import com.busco.localizacao.infra.redis.PublicadorDeEventoFake;
import com.busco.localizacao.support.EventoProcessadoEmMemoria;
import com.busco.localizacao.support.PosicaoReadModelEmMemoria;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Entrada do stream de posicoes de aluno. */
class PosicaoAlunoKafkaListenerTest {

    private static final String TOPICO = "tracking.posicao-aluno.v1";

    private PosicaoReadModelEmMemoria readModel;
    private PosicaoAlunoKafkaListener listener;

    @BeforeEach
    void montar() {
        readModel = new PosicaoReadModelEmMemoria();
        LocationProjectionService projecao = new LocationProjectionService(
                readModel, new GeoHashService(), new PublicadorDeEventoFake(), DurationDeTeste.JANELA_SEGUNDOS);
        listener = new PosicaoAlunoKafkaListener(projecao, new EventoProcessadoEmMemoria(),
                DurationDeTeste.TEMPO_LIMITE_MS);
    }

    @Test
    void projetaPosicaoValidaDoAlunoAssociadaAViagem() {
        PosicaoAlunoRegistrada evento = new PosicaoAlunoRegistrada(UUID.randomUUID().toString(),
                "viagem-42", "aluno-7", -23.5505, -46.6333, 1_700_000_000_000L, "2026-10-14T21:30:00Z");

        listener.consumir(registro(evento), new AckDeTeste());

        AlunoTrackingView view = readModel.aluno("aluno-7");
        assertNotNull(view, "a posicao do aluno deveria estar no read model");
        assertEquals("viagem-42", view.getViagemId(), "a posicao do aluno deveria ficar associada a viagem");
        assertEquals(-23.5505, view.getLatitude());
        assertEquals(-46.6333, view.getLongitude());
        assertEquals(1_700_000_000_000L, view.getTimestamp());
    }

    @Test
    void descartaEventoSemAlunoId() {
        PosicaoAlunoRegistrada evento = new PosicaoAlunoRegistrada(UUID.randomUUID().toString(),
                "viagem-42", " ", -23.5505, -46.6333, 1_700_000_000_000L, null);
        AckDeTeste ack = new AckDeTeste();

        listener.consumir(registro(evento), ack);

        assertTrue(readModel.vazio(), "evento sem aluno nao deveria ser projetado");
        assertTrue(ack.confirmado, "evento fora do contrato deveria ser descartado e confirmado");
    }

    @Test
    void descartaEventoComTimestampNaoPositivo() {
        PosicaoAlunoRegistrada evento = new PosicaoAlunoRegistrada(UUID.randomUUID().toString(),
                "viagem-42", "aluno-7", -23.5505, -46.6333, -1L, null);
        AckDeTeste ack = new AckDeTeste();

        listener.consumir(registro(evento), ack);

        assertTrue(readModel.vazio(), "evento sem timestamp valido nao deveria ser projetado");
        assertTrue(ack.confirmado, "evento fora do contrato deveria ser descartado e confirmado");
    }

    private ConsumerRecord<String, PosicaoAlunoRegistrada> registro(PosicaoAlunoRegistrada evento) {
        return new ConsumerRecord<>(TOPICO, 0, 0L, evento.alunoId(), evento);
    }
}
