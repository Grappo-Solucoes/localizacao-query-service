package com.busco.localizacao.infra.redis;

/**
 * Canal de tempo real por viagem: o que a projecao publica e o que a assinatura observa.
 *
 * <p>Nome e parse vivem juntos para que produtor e consumidor do canal nao divirjam.</p>
 */
public final class CanalDeViagem {

    /** Padrao assinado pelo servico; o identificador da viagem vem no nome do canal. */
    public static final String PADRAO = "tracking.viagem.*";

    private static final String PREFIXO = "tracking.viagem.";

    private CanalDeViagem() {
    }

    public static String nome(String viagemId) {
        return PREFIXO + viagemId;
    }

    /** Identificador da viagem no nome do canal, ou {@code null} quando o canal e de outro assunto. */
    public static String viagemId(String canal) {
        if (canal == null || !canal.startsWith(PREFIXO)) {
            return null;
        }

        String identificador = canal.substring(PREFIXO.length());
        return identificador.isBlank() ? null : identificador;
    }
}
