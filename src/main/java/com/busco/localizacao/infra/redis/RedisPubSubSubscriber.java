package com.busco.localizacao.infra.redis;

import com.busco.localizacao.websocket.SalasDeAcompanhamento;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Ponte entre o canal publicado (por qualquer instancia) e as conexoes locais.
 *
 * <p>O grupo de consumo do Kafka entrega cada posicao a uma instancia so; e por este canal que a
 * atualizacao alcanca tambem o cliente conectado nas outras instancias.</p>
 */
@Component
public class RedisPubSubSubscriber {

    private final ReactiveRedisMessageListenerContainer container;
    private final SalasDeAcompanhamento salas;

    public RedisPubSubSubscriber(
            ReactiveRedisMessageListenerContainer container,
            SalasDeAcompanhamento salas
    ) {
        this.container = container;
        this.salas = salas;
    }

    @PostConstruct
    public void subscribe() {

        container.receive(
                new PatternTopic(CanalDeViagem.PADRAO)
        )
                .flatMap(mensagem -> salas.publicar(
                        CanalDeViagem.viagemId(mensagem.getChannel()),
                        mensagem.getMessage()
                ))
                .subscribe();
    }
}
