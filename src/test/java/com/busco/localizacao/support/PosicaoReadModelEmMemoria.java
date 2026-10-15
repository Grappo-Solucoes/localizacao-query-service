package com.busco.localizacao.support;

import com.busco.localizacao.domain.AlunoTrackingView;
import com.busco.localizacao.domain.ViagemTrackingView;
import com.busco.localizacao.domain.repository.PosicaoReadRepository;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Read model em memoria para os testes de unidade: guarda a ultima posicao por entidade, sem
 * infraestrutura. O adaptador de Redis e o que roda em producao.
 */
public class PosicaoReadModelEmMemoria implements PosicaoReadRepository {

    private final Map<String, ViagemTrackingView> viagens = new ConcurrentHashMap<>();
    private final Map<String, AlunoTrackingView> alunos = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> salvarViagem(ViagemTrackingView view) {
        viagens.put(view.getViagemId(), view);
        return Mono.empty();
    }

    @Override
    public Mono<ViagemTrackingView> buscarViagem(String viagemId) {
        return Mono.justOrEmpty(viagens.get(viagemId));
    }

    @Override
    public Mono<Void> salvarAluno(AlunoTrackingView view) {
        alunos.put(view.getAlunoId(), view);
        return Mono.empty();
    }

    @Override
    public Mono<AlunoTrackingView> buscarAluno(String alunoId) {
        return Mono.justOrEmpty(alunos.get(alunoId));
    }

    public ViagemTrackingView viagem(String viagemId) {
        return viagens.get(viagemId);
    }

    public AlunoTrackingView aluno(String alunoId) {
        return alunos.get(alunoId);
    }

    /** {@code true} quando nenhuma posicao foi projetada. */
    public boolean vazio() {
        return viagens.isEmpty() && alunos.isEmpty();
    }
}
