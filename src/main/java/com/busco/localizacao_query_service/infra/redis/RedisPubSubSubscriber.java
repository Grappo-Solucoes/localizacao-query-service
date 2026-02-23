package com.busco.localizacao_query_service.infra.redis;

import com.busco.localizacao_query_service.websocket.RoomSocketHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class RedisPubSubSubscriber {

    private final ReactiveRedisMessageListenerContainer container;
    private final RoomSocketHandler socketHandler;

    public RedisPubSubSubscriber(
            ReactiveRedisMessageListenerContainer container,
            RoomSocketHandler socketHandler
    ) {
        this.container = container;
        this.socketHandler = socketHandler;
    }

    @PostConstruct
    public void subscribe() {

        Flux<?> flux = container.receive(
                new PatternTopic("tracking.viagem.*")
        );

        flux
                .cast(org.springframework.data.redis.connection.ReactiveSubscription.Message.class)
                .flatMap(message -> {

                    String channel = (String) message.getChannel();

                    String payload = (String) message.getMessage();

                    String viagemId =
                            channel.split("\\.")[2];

                    return socketHandler.broadcast(
                            viagemId,
                            payload
                    );
                })
                .subscribe();
    }
}