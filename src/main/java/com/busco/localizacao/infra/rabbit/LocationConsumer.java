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

    public LocationConsumer(
            LocationProjectionService projectionService
    ) {
        this.projectionService = projectionService;
    }

    @RabbitHandler
    @ConditionalOnExpression("#{headers['event-type'] == 'ViagemPosicaoAtualizadaEvent'}")
    public void consume(@Payload ViagemPosicaoAtualizadaEvent event) {
        projectionService
                .atualizarPosicao(event)
                .subscribe();
    }

    @RabbitHandler
    @ConditionalOnExpression("#{headers['event-type'] == 'AlunoPosicaoAtualizadaEvent'}")
    public void consume(@Payload AlunoPosicaoAtualizadaEvent event) {
        projectionService
                .atualizarPosicao(event)
                .subscribe();
    }
}