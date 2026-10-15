package com.busco.localizacao.app;

import com.busco.localizacao.app.geo.GeoHashService;
import com.busco.localizacao.domain.AlunoTrackingView;
import com.busco.localizacao.domain.ViagemTrackingView;
import com.busco.localizacao.domain.events.PosicaoAlunoRegistrada;
import com.busco.localizacao.domain.events.PosicaoVeiculoRegistrada;
import com.busco.localizacao.infra.redis.PublicadorDeEventoFake;
import com.busco.localizacao.support.PosicaoReadModelEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Projecao da posicao recebida no read model, com os campos do contrato v1. */
class LocationProjectionServiceTest {

    private static final long JANELA_PADRAO_SEGUNDOS = 120;

    private PosicaoReadModelEmMemoria readModel;
    private PublicadorDeEventoFake publisher;
    private LocationProjectionService projecao;

    @BeforeEach
    void montar() {
        readModel = new PosicaoReadModelEmMemoria();
        publisher = new PublicadorDeEventoFake();
        projecao = new LocationProjectionService(readModel, new GeoHashService(), publisher, JANELA_PADRAO_SEGUNDOS);
    }

    @Test
    void projetaAPosicaoDoVeiculoComOsCamposDoContrato() {
        PosicaoVeiculoRegistrada evento = new PosicaoVeiculoRegistrada("evt-1", "viagem-42",
                -23.5505, -46.6333, 1_700_000_000_000L, 88.5, 12.5, "gps", "2026-10-14T21:30:00Z");

        projecao.atualizarPosicao(evento).block();

        ViagemTrackingView view = readModel.viagem("viagem-42");
        assertEquals(-23.5505, view.getLatitude());
        assertEquals(-46.6333, view.getLongitude());
        assertEquals(1_700_000_000_000L, view.getTimestamp());
        assertEquals(88.5, view.getBateria());
        assertEquals(12.5, view.getPrecisao());
        assertEquals("gps", view.getProvedor());
    }

    @Test
    void projetaAPosicaoDoAlunoAssociadaAViagem() {
        PosicaoAlunoRegistrada evento = new PosicaoAlunoRegistrada("evt-2", "viagem-42", "aluno-7",
                -23.5505, -46.6333, 1_700_000_000_000L, "2026-10-14T21:30:00Z");

        projecao.atualizarPosicao(evento).block();

        AlunoTrackingView view = readModel.aluno("aluno-7");
        assertEquals("viagem-42", view.getViagemId());
        assertEquals(-23.5505, view.getLatitude());
        assertEquals(-46.6333, view.getLongitude());
        assertEquals(1_700_000_000_000L, view.getTimestamp());
    }

    @Test
    void gravaOGeohashDeOitoCaracteresNasPosicoesProjetadas() {
        projecao.atualizarPosicao(new PosicaoVeiculoRegistrada("evt-1", "viagem-42",
                -23.5505, -46.6333, 1_700_000_000_000L, null, null, null, null)).block();
        projecao.atualizarPosicao(new PosicaoAlunoRegistrada("evt-2", "viagem-42", "aluno-7",
                -23.5505, -46.6333, 1_700_000_000_000L, null)).block();

        assertEquals("6gyf4bf8", readModel.viagem("viagem-42").getGeohash());
        assertEquals(8, readModel.viagem("viagem-42").getGeohash().length());
        assertEquals("6gyf4bf8", readModel.aluno("aluno-7").getGeohash());
    }

    @Test
    void aplicaAJanelaConfiguradaAoProjetarAPosicao() {
        long antes = System.currentTimeMillis();

        projetarComJanela(Duration.ofSeconds(45));

        long expiraEm = readModel.viagem("viagem-42").getExpiraEm();
        assertTrue(expiraEm >= antes + 45_000,
                "expiraEm deveria contar a janela configurada a partir de agora: " + expiraEm);
        assertTrue(expiraEm <= System.currentTimeMillis() + 45_000,
                "expiraEm nao deveria passar da janela configurada: " + expiraEm);
    }

    @Test
    void posicaoProjetadaContinuaValendoDentroDaJanela() {
        projetarComJanela(Duration.ofSeconds(1));

        ViagemTrackingView viagem = readModel.viagem("viagem-42");
        AlunoTrackingView aluno = readModel.aluno("aluno-7");
        assertTrue(viagem.validaEm(System.currentTimeMillis()), "a posicao da viagem deveria valer agora");
        assertTrue(aluno.validaEm(System.currentTimeMillis()), "a posicao do aluno deveria valer agora");
        assertTrue(viagem.validaEm(viagem.getExpiraEm() - 1), "deveria valer um instante antes de expirar");
    }

    @Test
    void posicaoProjetadaNaoValeDepoisDaJanela() {
        projetarComJanela(Duration.ofSeconds(1));

        ViagemTrackingView viagem = readModel.viagem("viagem-42");
        AlunoTrackingView aluno = readModel.aluno("aluno-7");
        assertFalse(viagem.validaEm(viagem.getExpiraEm()), "a posicao nao deveria valer a partir de expiraEm");
        assertFalse(aluno.validaEm(aluno.getExpiraEm()), "a posicao do aluno tambem expira na mesma janela");
    }

    private void projetarComJanela(Duration janela) {
        projecao = new LocationProjectionService(readModel, new GeoHashService(), publisher, janela.toSeconds());
        projecao.atualizarPosicao(new PosicaoVeiculoRegistrada("evt-veiculo", "viagem-42",
                -23.5505, -46.6333, 1_700_000_000_000L, null, null, null, null)).block();
        projecao.atualizarPosicao(new PosicaoAlunoRegistrada("evt-aluno", "viagem-42", "aluno-7",
                -23.5505, -46.6333, 1_700_000_000_000L, null)).block();
    }

    @Test
    void publicaAAtualizacaoDaViagemParaQuemAcompanha() {
        PosicaoVeiculoRegistrada evento = new PosicaoVeiculoRegistrada("evt-1", "viagem-42",
                -23.5505, -46.6333, 1_700_000_000_000L, null, null, null, null);

        projecao.atualizarPosicao(evento).block();

        assertEquals(List.of("viagem-42"), publisher.viagensPublicadas());
    }
}
