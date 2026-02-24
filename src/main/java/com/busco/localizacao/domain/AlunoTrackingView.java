package com.busco.localizacao.domain;

import com.busco.localizacao.domain.events.AlunoPosicaoAtualizadaEvent;

public class AlunoTrackingView {

    private String alunoId;
    private String viagemId;
    private double latitude;
    private double longitude;
    private String geohash;
    private long timestamp;

    public Double distanciaDoOnibus; // distância até o ônibus da viagem em km
    public Long etaParaEmbarque; // tempo estimado para o ônibus chegar em segundos
    public Boolean aptoParaEmbarque; // true se está no ponto correto
    public String pontoEmbarqueId; // ID do ponto de embarque na rota
    public String pontoEmbarqueNome; // Nome do ponto (ex: "Escola Estadual")
    public String tipoEmbarque; // "EMBARQUE" ou "DESEMBARQUE"
    public Double distanciaAtePonto; // distância até o ponto de embarque em metros
    public Long horarioPrevisto; // horário previsto para embarque/desembarque
    public Boolean atrasado; // true se está atrasado em relação ao horário previsto
    public String status; // "AGUARDANDO", "NO_PONTO", "FORA_DO_PONTO", "ONIBUS_PROXIMO", "EMBARCADO", "DESEMBARCADO", "SEM_PONTO_DEFINIDO"
    public Double velocidadeOnibus; // velocidade atual do ônibus em km/h
    public Integer passageirosABordo; // número de passageiros (se disponível)
    public Double precisaoGPS; // precisão da localização em metros

    public AlunoTrackingView(String alunoId, String viagemId, double latitude, double longitude, String geohash, long timestamp, Double distanciaDoOnibus, Long etaParaEmbarque, Boolean aptoParaEmbarque, String pontoEmbarqueId, String pontoEmbarqueNome, String tipoEmbarque, Double distanciaAtePonto, Long horarioPrevisto, Boolean atrasado, String status, Double velocidadeOnibus, Integer passageirosABordo, Double precisaoGPS) {
        this.alunoId = alunoId;
        this.viagemId = viagemId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geohash = geohash;
        this.timestamp = timestamp;
        this.distanciaDoOnibus = distanciaDoOnibus;
        this.etaParaEmbarque = etaParaEmbarque;
        this.aptoParaEmbarque = aptoParaEmbarque;
        this.pontoEmbarqueId = pontoEmbarqueId;
        this.pontoEmbarqueNome = pontoEmbarqueNome;
        this.tipoEmbarque = tipoEmbarque;
        this.distanciaAtePonto = distanciaAtePonto;
        this.horarioPrevisto = horarioPrevisto;
        this.atrasado = atrasado;
        this.status = status;
        this.velocidadeOnibus = velocidadeOnibus;
        this.passageirosABordo = passageirosABordo;
        this.precisaoGPS = precisaoGPS;
    }

    public String getAlunoId() {
        return alunoId;
    }

    public String getViagemId() {
        return viagemId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getGeohash() {
        return geohash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Double getDistanciaDoOnibus() {
        return distanciaDoOnibus;
    }

    public Long getEtaParaEmbarque() {
        return etaParaEmbarque;
    }

    public Boolean getAptoParaEmbarque() {
        return aptoParaEmbarque;
    }

    public String getPontoEmbarqueId() {
        return pontoEmbarqueId;
    }

    public String getPontoEmbarqueNome() {
        return pontoEmbarqueNome;
    }

    public String getTipoEmbarque() {
        return tipoEmbarque;
    }

    public Double getDistanciaAtePonto() {
        return distanciaAtePonto;
    }

    public Long getHorarioPrevisto() {
        return horarioPrevisto;
    }

    public Boolean getAtrasado() {
        return atrasado;
    }

    public String getStatus() {
        return status;
    }

    public Double getVelocidadeOnibus() {
        return velocidadeOnibus;
    }

    public Integer getPassageirosABordo() {
        return passageirosABordo;
    }

    public Double getPrecisaoGPS() {
        return precisaoGPS;
    }

    public static AlunoTrackingView of(AlunoPosicaoAtualizadaEvent event, String geohash) {
        return new AlunoTrackingView(event.alunoId, event.viagemId, event.latitude, event.longitude, geohash, event.timestamp, event.distanciaDoOnibus, event.etaParaEmbarque,
                event.aptoParaEmbarque, event.pontoEmbarqueId, event.pontoEmbarqueNome, event.tipoEmbarque, event.distanciaAtePonto, event.horarioPrevisto, event.atrasado, event.status, event.velocidadeOnibus, event.passageirosABordo, event.precisaoGPS);
    }
}