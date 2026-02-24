package com.busco.localizacao.domain;

import com.busco.localizacao.domain.events.ViagemPosicaoAtualizadaEvent;

import java.io.Serializable;
import java.util.Map;

public class ViagemTrackingView implements Serializable {

    private String viagemId;
    private double latitude;
    private double longitude;
    private double velocidade;
    private String geohash;
    private long timestamp;

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

    public ViagemTrackingView() {}

    public ViagemTrackingView(String viagemId, double latitude, double longitude, double velocidade, String geohash, long timestamp, String endereco, Double distanciaParaDestino, Long etaSegundos, String status, Map<String, Object> metadata, Double accuracy, String provedor, Object bateria, String proximoPontoId, String proximoPontoNome, Double proximoPontoDistancia, Long proximoPontoETA, String proximoPontoTipo) {
        this.viagemId = viagemId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.velocidade = velocidade;
        this.geohash = geohash;
        this.timestamp = timestamp;
        this.endereco = endereco;
        this.distanciaParaDestino = distanciaParaDestino;
        this.etaSegundos = etaSegundos;
        this.status = status;
        this.metadata = metadata;
        this.accuracy = accuracy;
        this.provedor = provedor;
        this.bateria = bateria;
        this.proximoPontoId = proximoPontoId;
        this.proximoPontoNome = proximoPontoNome;
        this.proximoPontoDistancia = proximoPontoDistancia;
        this.proximoPontoETA = proximoPontoETA;
        this.proximoPontoTipo = proximoPontoTipo;
    }

    public String getViagemId() { return viagemId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getVelocidade() { return velocidade; }
    public String getGeohash() { return geohash; }
    public long getTimestamp() { return timestamp; }

    public String getEndereco() {
        return endereco;
    }

    public Double getDistanciaParaDestino() {
        return distanciaParaDestino;
    }

    public Long getEtaSegundos() {
        return etaSegundos;
    }

    public String getStatus() {
        return status;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public String getProvedor() {
        return provedor;
    }

    public Object getBateria() {
        return bateria;
    }

    public String getProximoPontoId() {
        return proximoPontoId;
    }

    public String getProximoPontoNome() {
        return proximoPontoNome;
    }

    public Double getProximoPontoDistancia() {
        return proximoPontoDistancia;
    }

    public Long getProximoPontoETA() {
        return proximoPontoETA;
    }

    public String getProximoPontoTipo() {
        return proximoPontoTipo;
    }

    public static ViagemTrackingView of(ViagemPosicaoAtualizadaEvent event, String geohash) {
        return new ViagemTrackingView(
                event.viagemId,
                event.latitude,
                event.longitude,
                event.velocidade,
                geohash,
                event.timestamp,
                event.endereco,
                event.distanciaParaDestino,
                event.etaSegundos,
                event.status,
                event.metadata,
                event.accuracy,
                event.provedor,
                event.bateria,
                event.proximoPontoId,
                event.proximoPontoNome,
                event.proximoPontoDistancia,
                event.proximoPontoETA,
                event.proximoPontoTipo
        );
    }
}