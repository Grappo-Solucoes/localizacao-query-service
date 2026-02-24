package com.busco.localizacao.domain.events;

public class AlunoPosicaoAtualizadaEvent {
    public String alunoId;
    public String viagemId;
    public double latitude;
    public double longitude;
    public long timestamp;
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
}
