package com.busco.localizacao.domain.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Contrato de entrada do topico {@code tracking.posicao-veiculo.v1}, como o
 * {@code tracking-ingestion-service} publica de fato.
 */
class PosicaoVeiculoRegistradaTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapeiaTodosOsCamposDoContratoV1DoVeiculo() throws Exception {
        String payload = """
                {"eventId":"7b0f5f6a-1c2b-4a8e-9f7d-2b3c4d5e6f70","viagemId":"viagem-42",
                 "latitude":-23.5505,"longitude":-46.6333,"timestamp":1700000000000,
                 "bateria":88.5,"precisao":12.5,"provedor":"gps",
                 "occurredOn":"2026-10-14T21:30:00Z"}
                """;

        PosicaoVeiculoRegistrada evento = mapper.readValue(payload, PosicaoVeiculoRegistrada.class);

        assertEquals("7b0f5f6a-1c2b-4a8e-9f7d-2b3c4d5e6f70", evento.eventId());
        assertEquals("viagem-42", evento.viagemId());
        assertEquals(-23.5505, evento.latitude());
        assertEquals(-46.6333, evento.longitude());
        assertEquals(1700000000000L, evento.timestamp());
        assertEquals(88.5, evento.bateria());
        assertEquals(12.5, evento.precisao());
        assertEquals("gps", evento.provedor());
        assertEquals("2026-10-14T21:30:00Z", evento.occurredOn());
    }

    @Test
    void camposOpcionaisAusentesViramNulo() throws Exception {
        String payload = """
                {"eventId":"7b0f5f6a-1c2b-4a8e-9f7d-2b3c4d5e6f70","viagemId":"viagem-42",
                 "latitude":-23.5505,"longitude":-46.6333,"timestamp":1700000000000,
                 "occurredOn":"2026-10-14T21:30:00Z"}
                """;

        PosicaoVeiculoRegistrada evento = mapper.readValue(payload, PosicaoVeiculoRegistrada.class);

        assertNull(evento.bateria());
        assertNull(evento.precisao());
        assertNull(evento.provedor());
    }

    @Test
    void ignoraCamposDesconhecidosDoVeiculo() throws Exception {
        String payload = """
                {"eventId":"7b0f5f6a-1c2b-4a8e-9f7d-2b3c4d5e6f70","viagemId":"viagem-42",
                 "latitude":-23.5505,"longitude":-46.6333,"timestamp":1700000000000,
                 "occurredOn":"2026-10-14T21:30:00Z","campoNovoDoProdutor":"qualquer"}
                """;

        PosicaoVeiculoRegistrada evento = mapper.readValue(payload, PosicaoVeiculoRegistrada.class);

        assertEquals("viagem-42", evento.viagemId());
    }
}
