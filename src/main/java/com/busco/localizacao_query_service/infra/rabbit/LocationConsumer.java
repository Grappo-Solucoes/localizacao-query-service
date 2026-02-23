package com.busco.localizacao_query_service.infra.rabbit;

import com.busco.localizacao_query_service.app.LocationProjectionService;
import com.busco.localizacao_query_service.infra.rabbit.event.ViagemPosicaoAtualizadaEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class LocationConsumer {

    private final LocationProjectionService projectionService;

    public LocationConsumer(
            LocationProjectionService projectionService
    ) {
        this.projectionService = projectionService;
    }

    @RabbitListener(queues = "location.query.queue")
    public void consume(ViagemPosicaoAtualizadaEvent event) {

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
}