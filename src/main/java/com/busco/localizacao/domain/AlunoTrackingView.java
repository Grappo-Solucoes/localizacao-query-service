package com.busco.localizacao.domain;

import com.busco.localizacao.domain.events.PosicaoAlunoRegistrada;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ultima posicao projetada de um aluno: apenas o que o contrato do topico {@code
 * tracking.posicao-aluno.v1} carrega, mais o geohash, a viagem associada e o instante de
 * expiracao (validade do read model).
 *
 * <p>O criador e explicito porque a classe nao tem construtor sem argumentos: o Jackson nao
 * escolheria um criador implicito sem os nomes dos parametros.</p>
 */
public class AlunoTrackingView {

    private final String alunoId;
    private final String viagemId;
    private final double latitude;
    private final double longitude;
    private final long timestamp;
    private final String geohash;
    private final long expiraEm;

    @JsonCreator
    public AlunoTrackingView(@JsonProperty("alunoId") String alunoId,
                             @JsonProperty("viagemId") String viagemId,
                             @JsonProperty("latitude") double latitude,
                             @JsonProperty("longitude") double longitude,
                             @JsonProperty("timestamp") long timestamp,
                             @JsonProperty("geohash") String geohash,
                             @JsonProperty("expiraEm") long expiraEm) {
        this.alunoId = alunoId;
        this.viagemId = viagemId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.geohash = geohash;
        this.expiraEm = expiraEm;
    }

    /**
     * Posicao do aluno vinda do topico Kafka: projeta somente o que o contrato v1 carrega,
     * mantendo a viagem associada.
     */
    public static AlunoTrackingView of(PosicaoAlunoRegistrada event, String geohash, long expiraEm) {
        return new AlunoTrackingView(
                event.alunoId(),
                event.viagemId(),
                event.latitude(),
                event.longitude(),
                event.timestamp(),
                geohash,
                expiraEm
        );
    }

    /** A posicao vale enquanto o instante informado for anterior a expiracao. */
    public boolean validaEm(long instante) {
        return instante < expiraEm;
    }

    public String getAlunoId() { return alunoId; }

    public String getViagemId() { return viagemId; }

    public double getLatitude() { return latitude; }

    public double getLongitude() { return longitude; }

    public long getTimestamp() { return timestamp; }

    public String getGeohash() { return geohash; }

    public long getExpiraEm() { return expiraEm; }
}
