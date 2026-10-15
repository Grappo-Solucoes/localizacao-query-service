package com.busco.localizacao.websocket;

/**
 * Caminho do handshake do acompanhamento: {@code /ws/viagem/{viagemId}}.
 */
public final class CaminhosDaViagem {

    private static final String PREFIXO = "/ws/viagem/";

    private CaminhosDaViagem() {
    }

    /**
     * Identificador da viagem no caminho, ou {@code null} quando o caminho nao segue o padrao — o
     * handshake fora do padrao e recusado em vez de virar um indice invalido.
     */
    public static String viagemId(String caminho) {
        if (caminho == null || !caminho.startsWith(PREFIXO)) {
            return null;
        }

        String identificador = caminho.substring(PREFIXO.length());
        if (identificador.isBlank() || identificador.contains("/")) {
            return null;
        }

        return identificador;
    }
}
