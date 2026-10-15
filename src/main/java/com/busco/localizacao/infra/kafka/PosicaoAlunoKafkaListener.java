package com.busco.localizacao.infra.kafka;

import com.busco.localizacao.app.LocationProjectionService;
import com.busco.localizacao.domain.events.PosicaoAlunoRegistrada;
import com.busco.localizacao.domain.repository.EventoProcessadoRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Entrada do stream de posicoes de aluno.
 *
 * <p>O commit do offset e manual e acontece depois de a projecao concluir: em caso de falha a
 * excecao sobe e o container reentrega (com retentativa limitada e dead letter). Evento fora do
 * contrato e descartado com log e confirmado, porque reentregar nao o tornaria valido.</p>
 */
@Component
public class PosicaoAlunoKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(PosicaoAlunoKafkaListener.class);

    private final LocationProjectionService projecao;
    private final EventoProcessadoRepository eventosProcessados;
    private final Duration tempoLimite;

    public PosicaoAlunoKafkaListener(LocationProjectionService projecao,
                                     EventoProcessadoRepository eventosProcessados,
                                     @Value("${tracking.tempo-limite-processamento-ms:15000}") long tempoLimiteMs) {
        this.projecao = projecao;
        this.eventosProcessados = eventosProcessados;
        this.tempoLimite = Duration.ofMillis(tempoLimiteMs);
    }

    @KafkaListener(topics = "${tracking.topico-aluno}", groupId = "${spring.kafka.consumer.group-id}",
            properties = "spring.json.value.default.type=com.busco.localizacao.domain.events.PosicaoAlunoRegistrada")
    public void consumir(ConsumerRecord<String, PosicaoAlunoRegistrada> registro, Acknowledgment ack) {
        PosicaoAlunoRegistrada evento = registro.value();

        if (foraDoContrato(evento)) {
            log.warn("Posicao do aluno descartada (topico {}, offset {}): evento fora do contrato",
                    registro.topic(), registro.offset());
            ack.acknowledge();
            return;
        }

        try {
            eventosProcessados.registrarSeNovo(evento.eventId())
                    .flatMap(primeiraEntrega -> {
                        if (!primeiraEntrega) {
                            log.info("Posicao do aluno {} ignorada (eventId {}): evento ja processado",
                                    evento.alunoId(), evento.eventId());
                            return Mono.empty();
                        }
                        return projecao.atualizarPosicao(evento);
                    })
                    .block(tempoLimite);
        } catch (RuntimeException e) {
            liberarMarcador(evento.eventId());
            throw e;
        }

        ack.acknowledge();
    }

    private boolean foraDoContrato(PosicaoAlunoRegistrada evento) {
        return evento == null
                || evento.eventId() == null || evento.eventId().isBlank()
                || evento.alunoId() == null || evento.alunoId().isBlank()
                || evento.viagemId() == null || evento.viagemId().isBlank()
                || evento.timestamp() <= 0;
    }

    /**
     * Libera o marcador de um evento cujo processamento falhou: sem isso a reentrega encontraria o
     * marcador e seria descartada como duplicata, deixando a falha silenciosa e o registro sem
     * chegar ao dead letter.
     */
    private void liberarMarcador(String eventId) {
        try {
            eventosProcessados.liberar(eventId).block(Duration.ofSeconds(2));
        } catch (RuntimeException e) {
            log.warn("Falha ao liberar o marcador do evento {}: {}", eventId, e.getMessage());
        }
    }
}
