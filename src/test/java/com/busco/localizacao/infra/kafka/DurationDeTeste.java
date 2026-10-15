package com.busco.localizacao.infra.kafka;

/** Tempo limite de processamento e janela do read model usados nos testes de unidade. */
final class DurationDeTeste {

    static final long TEMPO_LIMITE_MS = 5_000;
    static final long JANELA_SEGUNDOS = 120;

    private DurationDeTeste() {
    }
}
