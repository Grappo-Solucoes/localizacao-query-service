package com.busco.localizacao.domain.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Contrato de entrada do topico {@code tracking.posicao-aluno.v1} (v1), publicado pelo
 * {@code tracking-ingestion-service}. Chave da mensagem: {@code alunoId}.
 *
 * <p>O {@code viagemId} viaja junto para o consumidor correlacionar a posicao do aluno com a
 * viagem. Campos desconhecidos sao ignorados: o produtor pode acrescentar campos sem quebrar este
 * consumidor.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PosicaoAlunoRegistrada(String eventId,
                                     String viagemId,
                                     String alunoId,
                                     double latitude,
                                     double longitude,
                                     long timestamp,
                                     String occurredOn) {
}
