package com.busco.localizacao_query_service.app;

import com.busco.localizacao_query_service.app.geo.GeoHashService;
import com.busco.localizacao_query_service.domain.AlunoTrackingView;
import com.busco.localizacao_query_service.domain.ViagemTrackingView;
import com.busco.localizacao_query_service.infra.redis.AlunoTrackingRedisRepository;
import com.busco.localizacao_query_service.infra.redis.RedisPubSubPublisher;
import com.busco.localizacao_query_service.infra.redis.TrackingRedisRepository;
import com.busco.localizacao_query_service.websocket.RoomPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class LocationProjectionService {

    private final TrackingRedisRepository repository;
    private final AlunoTrackingRedisRepository alunoRepository;
    private final GeoHashService geoHashService;
    private final RedisPubSubPublisher redisPublisher;

    public LocationProjectionService(
            TrackingRedisRepository repository, AlunoTrackingRedisRepository alunoRepository,
            GeoHashService geoHashService,
            RedisPubSubPublisher redisPublisher
    ) {
        this.repository = repository;
        this.alunoRepository = alunoRepository;
        this.geoHashService = geoHashService;
        this.redisPublisher = redisPublisher;
    }

    public Mono<Void> atualizarPosicao(
            String viagemId,
            double lat,
            double lng,
            double velocidade,
            long timestamp
    ) {

        String geohash = geoHashService.generate(lat, lng);

        ViagemTrackingView view =
                new ViagemTrackingView(
                        viagemId,
                        lat,
                        lng,
                        velocidade,
                        geohash,
                        timestamp
                );

        return repository
                .save(view)
                .then(redisPublisher.publish(viagemId, new TrackingEvent("VIAGEM_POSICAO", view)));
    }

    public Mono<Void> atualizarPosicaoAluno(
            String alunoId,
            String viagemId,
            double lat,
            double lng,
            long timestamp
    ) {

        String geohash = geoHashService.generate(lat, lng);

        AlunoTrackingView view =
                new AlunoTrackingView(
                        alunoId,
                        viagemId,
                        lat,
                        lng,
                        geohash,
                        timestamp
                );

        return alunoRepository
                .save(view)
                .flatMap(v ->
                        redisPublisher.publish(
                                viagemId,
                                new TrackingEvent("ALUNO_POSICAO", view)
                        )
                );
    }
}