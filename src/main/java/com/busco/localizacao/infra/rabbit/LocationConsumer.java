package com.busco.localizacao.infra.rabbit;

import com.busco.localizacao.app.LocationProjectionService;
import com.busco.localizacao.domain.events.AlunoPosicaoAtualizadaEvent;
import com.busco.localizacao.domain.events.ViagemPosicaoAtualizadaEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = "location.query.queue")
public class LocationConsumer {

    private final LocationProjectionService projectionService;
    private final ObjectMapper mapper;

    public LocationConsumer(
            LocationProjectionService projectionService, ObjectMapper mapper
    ) {
        this.projectionService = projectionService;
        this.mapper = mapper;
    }

    @RabbitHandler
    @ConditionalOnExpression("#{headers['event-type'] == 'ViagemPosicaoAtualizadaEvent'}")
    public void consume(@Payload ViagemPosicaoAtualizadaEvent event) {
        projectionService
                .atualizarPosicao(
                        event.viagemId,
                        event.latitude,
                        event.longitude,
                        event.velocidade,
                        event.timestamp
                )
                .subscribe();
    }

    @RabbitHandler
    @ConditionalOnExpression("#{headers['event-type'] == 'AlunoPosicaoAtualizadaEvent'}")
    public void consume(@Payload AlunoPosicaoAtualizadaEvent event) {
        projectionService
                .atualizarPosicaoAluno(
                        event.alunoId,
                        event.viagemId,
                        event.latitude,
                        event.longitude,
                        event.timestamp
                )
                .subscribe();
    }
}