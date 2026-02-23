package com.busco.localizacao_query_service.infra.rabbit;

import com.busco.localizacao_query_service.app.LocationProjectionService;
import com.busco.localizacao_query_service.infra.rabbit.event.AlunoPosicaoAtualizadaEvent;
import com.busco.localizacao_query_service.infra.rabbit.event.ViagemPosicaoAtualizadaEvent;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = "location.query.queue")
public class LocationConsumer {

    private final LocationProjectionService projectionService;

    public LocationConsumer(
            LocationProjectionService projectionService
    ) {
        this.projectionService = projectionService;
    }

    @RabbitHandler
    @ConditionalOnExpression("#{headers['event-type'] == 'ViagemPosicaoAtualizadaEvent'}")
    public void consume(@Payload ViagemPosicaoAtualizadaEvent event
    ) {
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
    @ConditionalOnExpression("#{headers['event-type'] == 'ViagemPosicaoAtualizadaEvent'}")
    public void consume(@Payload  AlunoPosicaoAtualizadaEvent event) {
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