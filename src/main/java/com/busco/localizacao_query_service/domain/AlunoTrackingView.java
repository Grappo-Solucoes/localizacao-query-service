package com.busco.localizacao_query_service.domain;

public class AlunoTrackingView {

    private String alunoId;
    private String viagemId;
    private double latitude;
    private double longitude;
    private String geohash;
    private long timestamp;

    public AlunoTrackingView(
            String alunoId,
            String viagemId,
            double latitude,
            double longitude,
            String geohash,
            long timestamp
    ) {
        this.alunoId = alunoId;
        this.viagemId = viagemId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geohash = geohash;
        this.timestamp = timestamp;
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
}