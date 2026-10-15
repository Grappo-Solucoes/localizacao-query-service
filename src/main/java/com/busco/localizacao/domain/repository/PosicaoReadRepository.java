package com.busco.localizacao.domain.repository;

import com.busco.localizacao.domain.AlunoTrackingView;
import com.busco.localizacao.domain.ViagemTrackingView;
import reactor.core.publisher.Mono;

/**
 * Read model das posicoes projetadas: a ultima posicao conhecida por viagem e por aluno.
 *
 * <p>A implementacao vive em {@code infra/redis}, que e quem conhece a tecnologia de
 * armazenamento.</p>
 */
public interface PosicaoReadRepository {

    Mono<Void> salvarViagem(ViagemTrackingView view);

    Mono<ViagemTrackingView> buscarViagem(String viagemId);

    Mono<Void> salvarAluno(AlunoTrackingView view);

    Mono<AlunoTrackingView> buscarAluno(String alunoId);
}
