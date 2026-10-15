package com.busco.localizacao.infra.kafka;

import org.springframework.kafka.support.Acknowledgment;

/** Confirma se o deslocamento foi confirmado pelo listener. */
class AckDeTeste implements Acknowledgment {

    boolean confirmado;

    @Override
    public void acknowledge() {
        confirmado = true;
    }
}
