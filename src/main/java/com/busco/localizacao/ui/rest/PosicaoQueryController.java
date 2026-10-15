package com.busco.localizacao.ui.rest;

import com.busco.localizacao.domain.AlunoTrackingView;
import com.busco.localizacao.domain.ViagemTrackingView;
import com.busco.localizacao.domain.repository.PosicaoReadRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Consulta da ultima posicao projetada, por viagem e por aluno.
 *
 * <p>Posicao que nunca chegou ou que ja expirou nao e devolvida: a resposta sinaliza a ausencia em
 * vez de inventar coordenadas.</p>
 */
@RestController
@RequestMapping("/api/posicoes")
public class PosicaoQueryController {

    private final PosicaoReadRepository readModel;

    public PosicaoQueryController(PosicaoReadRepository readModel) {
        this.readModel = readModel;
    }

    @GetMapping("/viagem/{viagemId}")
    public Mono<ResponseEntity<ViagemTrackingView>> posicaoDaViagem(@PathVariable String viagemId) {
        return readModel.buscarViagem(viagemId)
                .filter(view -> view.validaEm(System.currentTimeMillis()))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/aluno/{alunoId}")
    public Mono<ResponseEntity<AlunoTrackingView>> posicaoDoAluno(@PathVariable String alunoId) {
        return readModel.buscarAluno(alunoId)
                .filter(view -> view.validaEm(System.currentTimeMillis()))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
