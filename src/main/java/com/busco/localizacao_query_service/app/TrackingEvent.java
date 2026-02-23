package com.busco.localizacao_query_service.app;

public class TrackingEvent {
    private String type;
    private Object payload;

    public TrackingEvent(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() { return type; }
    public Object getPayload() { return payload; }
}
