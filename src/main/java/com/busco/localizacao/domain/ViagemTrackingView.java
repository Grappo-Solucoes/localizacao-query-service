package com.busco.localizacao.domain;

import com.busco.localizacao.domain.events.PosicaoVeiculoRegistrada;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ultima posicao projetada de uma viagem: apenas o que o contrato do topico {@code
 * tracking.posicao-veiculo.v1} carrega, mais o geohash (consulta por regiao) e o instante de
 * expiracao (validade do read model).
 *
 * <p>O criador e explicito porque a classe nao tem construtor sem argumentos: o Jackson nao
 * escolheria um criador implicito sem os nomes dos parametros.</p>
 */
public class ViagemTrackingView {

    private final String viagemId;
    private final double latitude;
    private final double longitude;
    private final long timestamp;
    private final String geohash;
    private final long expiraEm;
    private final Double bateria;
    private final Double precisao;
    private final String provedor;

    @JsonCreator
    public ViagemTrackingView(@JsonProperty("viagemId") String viagemId,
                              @JsonProperty("latitude") double latitude,
                              @JsonProperty("longitude") double longitude,
                              @JsonProperty("timestamp") long timestamp,
                              @JsonProperty("geohash") String geohash,
                              @JsonProperty("expiraEm") long expiraEm,
                              @JsonProperty("bateria") Double bateria,
                              @JsonProperty("precisao") Double precisao,
                              @JsonProperty("provedor") String provedor) {
        this.viagemId = viagemId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.geohash = geohash;
        this.expiraEm = expiraEm;
        this.bateria = bateria;
        this.precisao = precisao;
        this.provedor = provedor;
    }

    /**
     * Posicao do veiculo vinda do topico Kafka: projeta somente o que o contrato v1 carrega.
     */
    public static ViagemTrackingView of(PosicaoVeiculoRegistrada event, String geohash, long expiraEm) {
        return new ViagemTrackingView(
                event.viagemId(),
                event.latitude(),
                event.longitude(),
                event.timestamp(),
                geohash,
                expiraEm,
                event.bateria(),
                event.precisao(),
                event.provedor()
        );
    }

    /** A posicao vale enquanto o instante informado for anterior a expiracao. */
    public boolean validaEm(long instante) {
        return instante < expiraEm;
    }

    public String getViagemId() { return viagemId; }

    public double getLatitude() { return latitude; }

    public double getLongitude() { return longitude; }

    public long getTimestamp() { return timestamp; }

    public String getGeohash() { return geohash; }

    public long getExpiraEm() { return expiraEm; }

    public Double getBateria() { return bateria; }

    public Double getPrecisao() { return precisao; }

    public String getProvedor() { return provedor; }
}
