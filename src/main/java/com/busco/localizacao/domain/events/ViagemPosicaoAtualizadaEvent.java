package com.busco.localizacao.domain.events;

import java.util.Map;

public class ViagemPosicaoAtualizadaEvent {

    public String viagemId;
    public double latitude;
    public double longitude;
    public long timestamp;
    public double velocidade;
    public String endereco;
    public Double distanciaParaDestino;
    public Long etaSegundos;
    public String status; // "EM_ROTA", "PROXIMO", "CHEGOU", "PARADO"
    public Map<String, Object> metadata;
    public Double accuracy;
    public String provedor;
    public Object bateria;

    public String proximoPontoId;
    public String proximoPontoNome;
    public Double proximoPontoDistancia; // em metros
    public Long proximoPontoETA; // em segundos
    public String proximoPontoTipo; // TERMINAL, ESCOLA, PARADA

}
