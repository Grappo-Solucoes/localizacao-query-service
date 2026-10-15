package com.busco.localizacao.infra.kafka;

import com.busco.localizacao.app.LocationProjectionService;
import com.busco.localizacao.app.geo.GeoHashService;
import com.busco.localizacao.domain.ViagemTrackingView;
import com.busco.localizacao.domain.events.PosicaoVeiculoRegistrada;
import com.busco.localizacao.infra.redis.PublicadorDeEventoFake;
import com.busco.localizacao.support.EventoProcessadoEmMemoria;
import com.busco.localizacao.support.PosicaoReadModelEmMemoria;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Entrada do stream de posicoes do veiculo. */
class PosicaoVeiculoKafkaListenerTest {

    private static final String TOPICO = "tracking.posicao-veiculo.v1";

    private PosicaoReadModelEmMemoria readModel;
    private PosicaoVeiculoKafkaListener listener;

    @BeforeEach
    void montar() {
        readModel = new PosicaoReadModelEmMemoria();
        LocationProjectionService projecao = new LocationProjectionService(
                readModel, new GeoHashService(), new PublicadorDeEventoFake(), DurationDeTeste.JANELA_SEGUNDOS);
        listener = new PosicaoVeiculoKafkaListener(projecao, new EventoProcessadoEmMemoria(),
                DurationDeTeste.TEMPO_LIMITE_MS);
    }

    @Test
    void projetaPosicaoValidaDoVeiculo() {
        PosicaoVeiculoRegistrada evento = new PosicaoVeiculoRegistrada(UUID.randomUUID().toString(),
                "viagem-42", -23.5505, -46.6333, 1_700_000_000_000L, 88.5, 12.5, "gps", "2026-10-14T21:30:00Z");

        listener.consumir(registro(evento), ack());

        ViagemTrackingView view = readModel.viagem("viagem-42");
        assertNotNull(view, "a posicao do veiculo deveria estar no read model");
        assertEquals(-23.5505, view.getLatitude());
        assertEquals(-46.6333, view.getLongitude());
        assertEquals(1_700_000_000_000L, view.getTimestamp());
    }

    @Test
    void confirmaODeslocamentoDepoisDeProjetar() {
        PosicaoVeiculoRegistrada evento = new PosicaoVeiculoRegistrada(UUID.randomUUID().toString(),
                "viagem-42", -23.5505, -46.6333, 1_700_000_000_000L, null, null, null, null);
        AckDeTeste ack = new AckDeTeste();

        listener.consumir(registro(evento), ack);

        assertTrue(ack.confirmado, "o deslocamento deveria ser confirmado depois da projecao");
    }

    @Test
    void descartaEventoSemEventId() {
        PosicaoVeiculoRegistrada evento = new PosicaoVeiculoRegistrada(null,
                "viagem-42", -23.5505, -46.6333, 1_700_000_000_000L, null, null, null, null);
        AckDeTeste ack = new AckDeTeste();

        listener.consumir(registro(evento), ack);

        assertTrue(readModel.vazio(), "evento sem identificacao nao deveria ser projetado");
        assertTrue(ack.confirmado, "evento fora do contrato deveria ser descartado e confirmado");
    }

    @Test
    void descartaEventoSemViagemId() {
        PosicaoVeiculoRegistrada evento = new PosicaoVeiculoRegistrada(UUID.randomUUID().toString(),
                "  ", -23.5505, -46.6333, 1_700_000_000_000L, null, null, null, null);
        AckDeTeste ack = new AckDeTeste();

        listener.consumir(registro(evento), ack);

        assertTrue(readModel.vazio(), "evento sem viagem nao deveria ser projetado");
        assertTrue(ack.confirmado, "evento fora do contrato deveria ser descartado e confirmado");
    }

    @Test
    void descartaEventoComTimestampNaoPositivo() {
        PosicaoVeiculoRegistrada evento = new PosicaoVeiculoRegistrada(UUID.randomUUID().toString(),
                "viagem-42", -23.5505, -46.6333, 0L, null, null, null, null);
        AckDeTeste ack = new AckDeTeste();

        listener.consumir(registro(evento), ack);

        assertTrue(readModel.vazio(), "evento sem timestamp valido nao deveria ser projetado");
        assertTrue(ack.confirmado, "evento fora do contrato deveria ser descartado e confirmado");
    }

    @Test
    void ignoraReentregaDoMesmoEvento() {
        String eventId = UUID.randomUUID().toString();
        PosicaoVeiculoRegistrada primeira = new PosicaoVeiculoRegistrada(eventId,
                "viagem-42", -23.5505, -46.6333, 1_700_000_000_000L, null, null, null, null);
        PosicaoVeiculoRegistrada repetida = new PosicaoVeiculoRegistrada(eventId,
                "viagem-42", -22.0000, -43.0000, 1_700_000_009_000L, null, null, null, null);

        listener.consumir(registro(primeira), new AckDeTeste());
        listener.consumir(registro(repetida), new AckDeTeste());

        ViagemTrackingView view = readModel.viagem("viagem-42");
        assertEquals(-23.5505, view.getLatitude(), "a reentrega do mesmo eventId nao deveria reprojetar");
        assertEquals(1_700_000_000_000L, view.getTimestamp());
    }

    @Test
    void liberaMarcadorQuandoAProjecaoFalha() {
        PosicaoReadModelEmMemoria readModelQueFalhaNaPrimeiraVez = new PosicaoReadModelEmMemoria() {
            private int tentativas;

            @Override
            public Mono<Void> salvarViagem(ViagemTrackingView view) {
                if (++tentativas == 1) {
                    return Mono.error(new IllegalStateException("falha injetada na projecao"));
                }
                return super.salvarViagem(view);
            }
        };
        PosicaoVeiculoKafkaListener listenerComFalha = new PosicaoVeiculoKafkaListener(
                new LocationProjectionService(readModelQueFalhaNaPrimeiraVez, new GeoHashService(),
                        new PublicadorDeEventoFake(), DurationDeTeste.JANELA_SEGUNDOS),
                new EventoProcessadoEmMemoria(), DurationDeTeste.TEMPO_LIMITE_MS);
        PosicaoVeiculoRegistrada evento = new PosicaoVeiculoRegistrada(UUID.randomUUID().toString(),
                "viagem-42", -23.5505, -46.6333, 1_700_000_000_000L, null, null, null, null);

        assertThrows(RuntimeException.class,
                () -> listenerComFalha.consumir(registro(evento), new AckDeTeste()),
                "a falha da projecao deveria subir para o container reentregar o registro");

        listenerComFalha.consumir(registro(evento), new AckDeTeste());

        assertNotNull(readModelQueFalhaNaPrimeiraVez.viagem("viagem-42"),
                "a reentrega deveria processar o evento depois de o marcador ser liberado");
    }

    private ConsumerRecord<String, PosicaoVeiculoRegistrada> registro(PosicaoVeiculoRegistrada evento) {
        return new ConsumerRecord<>(TOPICO, 0, 0L, evento.viagemId(), evento);
    }

    private Acknowledgment ack() {
        return new AckDeTeste();
    }
}
