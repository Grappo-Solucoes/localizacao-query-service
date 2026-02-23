package com.busco.localizacao_query_service.domain;

import java.io.Serializable;

public class ViagemTrackingView implements Serializable {

    private String viagemId;
    private double latitude;
    private double longitude;
    private double velocidade;
    private String geohash;
    private long timestamp;

    public ViagemTrackingView() {}

    public ViagemTrackingView(
            String viagemId,
            double latitude,
            double longitude,
            double velocidade,
            String geohash,
            long timestamp
    ) {
        this.viagemId = viagemId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.velocidade = velocidade;
        this.geohash = geohash;
        this.timestamp = timestamp;
    }

    public String getViagemId() { return viagemId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getVelocidade() { return velocidade; }
    public String getGeohash() { return geohash; }
    public long getTimestamp() { return timestamp; }
}