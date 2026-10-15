package com.busco.localizacao.websocket;

import com.busco.localizacao.infra.redis.CanalDeViagem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Salas de acompanhamento: entrega por viagem, ciclo de vida e contagem de conexoes. */
class SalasDeAcompanhamentoTest {

    private static final String POSICAO = "{\"type\":\"VIAGEM_POSICAO\"}";

    private SalasDeAcompanhamento salas;
    private final List<Disposable> conexoes = new CopyOnWriteArrayList<>();

    @BeforeEach
    void montar() {
        salas = new SalasDeAcompanhamento();
    }

    @AfterEach
    void encerrar() {
        conexoes.forEach(Disposable::dispose);
    }

    @Test
    void entregaOPayloadAosAssinantesDaViagem() {
        List<String> recebidos = assinar("viagem-42");

        salas.publicar("viagem-42", POSICAO).block();

        assertEquals(List.of(POSICAO), recebidos);
    }

    @Test
    void naoEntregaParaAssinanteDeOutraViagem() {
        List<String> recebidosPor42 = assinar("viagem-42");
        List<String> recebidosPor43 = assinar("viagem-43");

        salas.publicar("viagem-42", POSICAO).block();

        assertEquals(List.of(POSICAO), recebidosPor42);
        assertTrue(recebidosPor43.isEmpty(), "a viagem 43 nao deveria receber a posicao da viagem 42");
    }

    @Test
    void liberaASalaQuandoAUltimaConexaoEncerra() {
        Disposable conexao = salas.entrar("viagem-42").subscribe(ignorado -> { });
        conexoes.add(conexao);

        assertEquals(1, salas.salasAtivas());
        assertEquals(1, salas.conexoes());

        conexao.dispose();

        assertEquals(0, salas.salasAtivas(), "a sala deveria sair do mapa quando a conexao encerra");
        assertEquals(0, salas.conexoes());
    }

    @Test
    void mantemASalaEnquantoHouberOutraConexao() {
        Disposable primeira = salas.entrar("viagem-42").subscribe(ignorado -> { });
        Disposable segunda = salas.entrar("viagem-42").subscribe(ignorado -> { });
        conexoes.add(segunda);

        primeira.dispose();

        assertEquals(1, salas.salasAtivas(), "outra conexao da mesma viagem mantem a sala");
        assertEquals(1, salas.conexoes());
    }

    @Test
    void entregaAoClienteDeOutraInstanciaPeloCanalPublicado() {
        SalasDeAcompanhamento instanciaQueProjeta = new SalasDeAcompanhamento();
        SalasDeAcompanhamento instanciaDoCliente = new SalasDeAcompanhamento();
        List<String> recebidosPeloCliente = assinarEm(instanciaDoCliente, "viagem-42");

        // O canal que a instancia que projeta publica e o mesmo que a assinatura da outra instancia
        // observa: o identificador volta pelo parse do canal.
        String canal = CanalDeViagem.nome("viagem-42");
        instanciaDoCliente.publicar(CanalDeViagem.viagemId(canal), POSICAO).block();

        assertEquals(List.of(POSICAO), recebidosPeloCliente);
        assertEquals(0, instanciaQueProjeta.conexoes());
    }

    private List<String> assinar(String viagemId) {
        return assinarEm(salas, viagemId);
    }

    private List<String> assinarEm(SalasDeAcompanhamento instancia, String viagemId) {
        List<String> recebidos = new CopyOnWriteArrayList<>();
        Disposable conexao = instancia.entrar(viagemId).subscribe(recebidos::add);
        conexoes.add(conexao);
        return recebidos;
    }
}
