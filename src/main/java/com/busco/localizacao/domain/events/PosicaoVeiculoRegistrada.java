package com.busco.localizacao.domain.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Contrato de entrada do topico {@code tracking.posicao-veiculo.v1} (v1), publicado pelo
 * {@code tracking-ingestion-service}. Chave da mensagem: {@code viagemId}.
 *
 * <p>{@code timestamp} e o relogio do aparelho, em epoch millis. {@code occurredOn} e o instante de
 * recebimento no ingestor e fica como texto porque e diagnostico, nao alimenta o read model.
 * Campos desconhecidos sao ignorados: o produtor pode acrescentar campos sem quebrar este
 * consumidor.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PosicaoVeiculoRegistrada(String eventId,
                                       String viagemId,
                                       double latitude,
                                       double longitude,
                                       long timestamp,
                                       Double bateria,
                                       Double precisao,
                                       String provedor,
                                       String occurredOn) {
}
