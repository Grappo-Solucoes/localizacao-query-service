package com.busco.localizacao.app;

import com.busco.localizacao.app.geo.GeoHashService;
import com.busco.localizacao.domain.AlunoTrackingView;
import com.busco.localizacao.domain.ViagemTrackingView;
import com.busco.localizacao.domain.events.AlunoPosicaoAtualizadaEvent;
import com.busco.localizacao.domain.events.ViagemPosicaoAtualizadaEvent;
import com.busco.localizacao.infra.redis.AlunoTrackingRedisRepository;
import com.busco.localizacao.infra.redis.RedisPubSubPublisher;
import com.busco.localizacao.infra.redis.TrackingRedisRepository;
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
            ViagemPosicaoAtualizadaEvent event
    ) {

        String geohash = geoHashService.generate(event.latitude, event.longitude);

        ViagemTrackingView view =
                ViagemTrackingView.of(event, geohash);

        return repository
                .save(view)
                .then(redisPublisher.publish(event.viagemId, new TrackingEvent("VIAGEM_POSICAO", view)));
    }

    public Mono<Void> atualizarPosicao(
            AlunoPosicaoAtualizadaEvent event
    ) {

        String geohash = geoHashService.generate(event.latitude, event.longitude);

        AlunoTrackingView view =
                 AlunoTrackingView.of(
                        event,
                        geohash
                );

        return alunoRepository
                .save(view)
                .flatMap(v ->
                        redisPublisher.publish(
                                event.viagemId,
                                new TrackingEvent("ALUNO_POSICAO", view)
                        )
                );
    }
}