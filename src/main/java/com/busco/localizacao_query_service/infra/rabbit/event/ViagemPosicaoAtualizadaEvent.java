package com.busco.localizacao_query_service.infra.rabbit.event;
public class ViagemPosicaoAtualizadaEvent {

    public String viagemId;
    public double latitude;
    public double longitude;
    public double velocidade;
    public long timestamp;
}
