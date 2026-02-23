package com.busco.localizacao_query_service.app;

import com.busco.localizacao_query_service.app.geo.GeoHashService;
import com.busco.localizacao_query_service.domain.ViagemTrackingView;
import com.busco.localizacao_query_service.infra.redis.TrackingRedisRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class LocationProjectionService {

    private final TrackingRedisRepository repository;
    private final GeoHashService geoHashService;
    private final RoomPublisher roomPublisher;

    public LocationProjectionService(
            TrackingRedisRepository repository,
            GeoHashService geoHashService,
            RoomPublisher roomPublisher
    ) {
        this.repository = repository;
        this.geoHashService = geoHashService;
        this.roomPublisher = roomPublisher;
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
                .then(roomPublisher.publish(viagemId, view));
    }
}