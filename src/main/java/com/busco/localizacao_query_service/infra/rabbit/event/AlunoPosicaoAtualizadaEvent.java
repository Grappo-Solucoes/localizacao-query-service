package com.busco.localizacao_query_service.infra.rabbit.event;

public class AlunoPosicaoAtualizadaEvent {
    public String alunoId;
    public String viagemId;
    public double latitude;
    public double longitude;
    public long timestamp;
}
