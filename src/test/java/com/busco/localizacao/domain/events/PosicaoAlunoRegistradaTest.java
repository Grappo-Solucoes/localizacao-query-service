package com.busco.localizacao.domain.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contrato de entrada do topico {@code tracking.posicao-aluno.v1}: o aluno e a viagem viajam
 * juntos para o consumidor correlacionar os dois.
 */
class PosicaoAlunoRegistradaTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapeiaTodosOsCamposDoContratoV1DoAluno() throws Exception {
        String payload = """
                {"eventId":"9d1e2f3a-4b5c-6d7e-8f90-1a2b3c4d5e6f","viagemId":"viagem-42",
                 "alunoId":"aluno-7","latitude":-23.5505,"longitude":-46.6333,
                 "timestamp":1700000000000,"occurredOn":"2026-10-14T21:30:00Z"}
                """;

        PosicaoAlunoRegistrada evento = mapper.readValue(payload, PosicaoAlunoRegistrada.class);

        assertEquals("9d1e2f3a-4b5c-6d7e-8f90-1a2b3c4d5e6f", evento.eventId());
        assertEquals("viagem-42", evento.viagemId());
        assertEquals("aluno-7", evento.alunoId());
        assertEquals(-23.5505, evento.latitude());
        assertEquals(-46.6333, evento.longitude());
        assertEquals(1700000000000L, evento.timestamp());
        assertEquals("2026-10-14T21:30:00Z", evento.occurredOn());
    }

    @Test
    void ignoraCamposDesconhecidosDoAluno() throws Exception {
        String payload = """
                {"eventId":"9d1e2f3a-4b5c-6d7e-8f90-1a2b3c4d5e6f","viagemId":"viagem-42",
                 "alunoId":"aluno-7","latitude":-23.5505,"longitude":-46.6333,
                 "timestamp":1700000000000,"occurredOn":"2026-10-14T21:30:00Z",
                 "campoNovoDoProdutor":"qualquer"}
                """;

        PosicaoAlunoRegistrada evento = mapper.readValue(payload, PosicaoAlunoRegistrada.class);

        assertEquals("aluno-7", evento.alunoId());
    }
}
