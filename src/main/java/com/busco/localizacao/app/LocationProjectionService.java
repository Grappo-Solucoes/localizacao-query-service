package com.busco.localizacao.app;

import com.busco.localizacao.app.geo.GeoHashService;
import com.busco.localizacao.domain.AlunoTrackingView;
import com.busco.localizacao.domain.ViagemTrackingView;
import com.busco.localizacao.domain.events.PosicaoAlunoRegistrada;
import com.busco.localizacao.domain.events.PosicaoVeiculoRegistrada;
import com.busco.localizacao.domain.repository.PosicaoReadRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Projeta a posicao recebida do Kafka no read model e avisa quem acompanha a viagem.
 */
@Service
public class LocationProjectionService {

    private final PosicaoReadRepository readModel;
    private final GeoHashService geoHashService;
    private final PosicaoPublisher publisher;
    private final Duration janela;

    public LocationProjectionService(
            PosicaoReadRepository readModel,
            GeoHashService geoHashService,
            PosicaoPublisher publisher,
            @Value("${tracking.posicao.ttl-segundos:120}") long janelaSegundos
    ) {
        this.readModel = readModel;
        this.geoHashService = geoHashService;
        this.publisher = publisher;
        this.janela = Duration.ofSeconds(janelaSegundos);
    }

    public Mono<Void> atualizarPosicao(
            PosicaoVeiculoRegistrada evento
    ) {
        ViagemTrackingView view = ViagemTrackingView.of(
                evento, geoHashService.generate(evento.latitude(), evento.longitude()), expiraEm());

        return readModel
                .salvarViagem(view)
                .then(publisher.publish(evento.viagemId(), new TrackingEvent("VIAGEM_POSICAO", view)));
    }

    public Mono<Void> atualizarPosicao(
            PosicaoAlunoRegistrada evento
    ) {
        AlunoTrackingView view = AlunoTrackingView.of(
                evento, geoHashService.generate(evento.latitude(), evento.longitude()), expiraEm());

        return readModel
                .salvarAluno(view)
                .then(publisher.publish(evento.viagemId(), new TrackingEvent("ALUNO_POSICAO", view)));
    }

    /**
     * A janela conta do instante da projecao, nao do relogio do aparelho: um aparelho com relogio
     * atrasado nao pode encurtar a validade da posicao no read model.
     */
    private long expiraEm() {
        return System.currentTimeMillis() + janela.toMillis();
    }
}
