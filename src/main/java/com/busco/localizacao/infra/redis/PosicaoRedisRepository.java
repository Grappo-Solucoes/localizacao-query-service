package com.busco.localizacao.infra.redis;

import com.busco.localizacao.domain.AlunoTrackingView;
import com.busco.localizacao.domain.ViagemTrackingView;
import com.busco.localizacao.domain.repository.PosicaoReadRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Read model em Redis: a ultima posicao por viagem e por aluno.
 *
 * <p>Uma janela unica de validade, a mesma que a posicao carrega em {@code expiraEm}: o Redis
 * libera a chave e a consulta deixa de encontrar a posicao.</p>
 */
@Repository
public class PosicaoRedisRepository implements PosicaoReadRepository {

    private static final String PREFIXO_VIAGEM = "tracking:viagem:";
    private static final String PREFIXO_ALUNO = "tracking:aluno:";

    private final ReactiveRedisTemplate<String, ViagemTrackingView> viagemTemplate;
    private final ReactiveRedisTemplate<String, AlunoTrackingView> alunoTemplate;
    private final Duration janela;

    public PosicaoRedisRepository(
            @Qualifier("viagemTrackingRedisTemplate") ReactiveRedisTemplate<String, ViagemTrackingView> viagemTemplate,
            @Qualifier("alunoTrackingRedisTemplate") ReactiveRedisTemplate<String, AlunoTrackingView> alunoTemplate,
            @Value("${tracking.posicao.ttl-segundos:120}") long janelaSegundos) {
        this.viagemTemplate = viagemTemplate;
        this.alunoTemplate = alunoTemplate;
        this.janela = Duration.ofSeconds(janelaSegundos);
    }

    @Override
    public Mono<Void> salvarViagem(ViagemTrackingView view) {
        return viagemTemplate.opsForValue()
                .set(PREFIXO_VIAGEM + view.getViagemId(), view, janela)
                .then();
    }

    @Override
    public Mono<ViagemTrackingView> buscarViagem(String viagemId) {
        return viagemTemplate.opsForValue().get(PREFIXO_VIAGEM + viagemId);
    }

    @Override
    public Mono<Void> salvarAluno(AlunoTrackingView view) {
        return alunoTemplate.opsForValue()
                .set(PREFIXO_ALUNO + view.getAlunoId(), view, janela)
                .then();
    }

    @Override
    public Mono<AlunoTrackingView> buscarAluno(String alunoId) {
        return alunoTemplate.opsForValue().get(PREFIXO_ALUNO + alunoId);
    }
}
