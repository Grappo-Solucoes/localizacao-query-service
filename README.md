# localizacao-query-service

Lado **query** das posições de rastreamento: consome as posições publicadas no Kafka, mantém a
última posição de cada viagem e de cada aluno e serve as consultas (leitura e acompanhamento em
tempo real por viagem).

O lado **command** — o pipeline que calcula velocidade, ETA, geofence e detecção de parada — é o
`localizacao-core-service`. As posições nascem no app, são lidas do Firebase pelo
`tracking-ingestion-service` e chegam aqui pelo Kafka.

## Contrato consumido

| Tópico | Chave | Campos |
|---|---|---|
| `tracking.posicao-veiculo.v1` | `viagemId` | `eventId`, `viagemId`, `latitude`, `longitude`, `timestamp`, `bateria`, `precisao`, `provedor`, `occurredOn` |
| `tracking.posicao-aluno.v1` | `alunoId` | `eventId`, `viagemId`, `alunoId`, `latitude`, `longitude`, `timestamp`, `occurredOn` |

- `timestamp` é o relógio do aparelho, em epoch millis; `occurredOn` é o instante de recebimento no
  ingestor (ISO-8601) e serve só para diagnóstico.
- `bateria`, `precisao` e `provedor` são opcionais. O app não grava `provider`, então `provedor`
  chega nulo.
- Campos desconhecidos são ignorados: o produtor pode acrescentar campos sem quebrar este consumidor.
- Grupo de consumo: `localizacao-query`, próprio deste serviço e **diferente** do `localizacao-core`.
  Com o mesmo grupo, os dois serviços dividiriam as partições e cada um veria metade das posições.
- O deslocamento é confirmado (commit manual) só depois de a projeção concluir. Evento fora do
  contrato — sem `eventId`, sem `viagemId`/`alunoId` ou com `timestamp` não positivo — é descartado e
  confirmado, porque reentregar não o tornaria válido. Registro que falha é reentregue com espera
  limitada e, esgotadas as tentativas, vai para `<tópico>.DLT`.
- `auto-offset-reset=latest`: o read model guarda o "agora", não histórico. Na primeira subida, uma
  posição publicada entre a abertura da porta HTTP e a atribuição de partições do consumidor é
  pulada — algumas posições contínuas chegam logo depois e repõem o estado.

O que **não** vem nesse contrato e por isso não existe no read model: `velocidade`, `endereco`,
`status`, `proximoPonto*`, `etaSegundos` e, no caso do aluno, `distanciaDoOnibus`,
`aptoParaEmbarque`, `tipoEmbarque`, `horarioPrevisto` e `precisaoGPS`. Esses campos só existem no
evento enriquecido publicado pelo `localizacao-core-service`.

Consequência nas respostas: elas trazem apenas o que o tópico carrega — coordenadas, `timestamp`,
`geohash` e `expiraEm`, mais `bateria`, `precisao` e `provedor` no caso do veículo. Os campos acima
**não aparecem** na resposta (não são campos vazios: eles não existem nas views).

## Consulta

| Endpoint | Resposta |
|---|---|
| `GET /api/posicoes/viagem/{viagemId}` | `200` com a última posição do veículo, ou `404` quando não há posição válida |
| `GET /api/posicoes/aluno/{alunoId}` | `200` com a última posição do aluno, incluindo o `viagemId` associado, ou `404` quando não há posição válida |

    curl -s localhost:8080/api/posicoes/viagem/42
    {"viagemId":"42","latitude":-23.5505,"longitude":-46.6333,"timestamp":1700000000000,"geohash":"6gyf4bf8","expiraEm":1700000060000,"bateria":88.5,"precisao":12.5,"provedor":"gps"}

    curl -s localhost:8080/api/posicoes/aluno/7
    {"alunoId":"7","viagemId":"42","latitude":-23.5505,"longitude":-46.6333,"timestamp":1700000000000,"geohash":"6gyf4bf8","expiraEm":1700000060000}

O `404` significa "nunca chegou posição para essa entidade, ou ela já expirou": a posição vale pela
janela de `TRACKING_POSICAO_TTL_SEGUNDOS` contada do instante da projeção, que é o `expiraEm` da
resposta.

## Como subir

Broker e Redis locais:

    docker compose -f docker-compose.dev.yml up -d

Serviço (o `REDIS_HOST` default é `localhost`, que é onde o compose publica o Redis):

    ./mvnw spring-boot:run

Testes:

    ./mvnw test

Os testes de unidade não precisam de infraestrutura. O teste de dead letter
(`KafkaDltIntegrationTest`) sobe um broker Kafka embutido na própria JVM.

## Variáveis de ambiente

| Variável | Default | Para que serve |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | broker Kafka |
| `KAFKA_GROUP_ID` | `localizacao-query` | grupo de consumo |
| `TRACKING_TOPIC` | `tracking.posicao-veiculo.v1` | tópico das posições do veículo |
| `TRACKING_ALUNO_TOPIC` | `tracking.posicao-aluno.v1` | tópico das posições do aluno |
| `TRACKING_TIMEOUT_MS` | `15000` | tempo limite do processamento de um registro |
| `TRACKING_POSICAO_TTL_SEGUNDOS` | `120` | janela de validade do read model |
| `TRACKING_IDEMPOTENCIA_TTL_MIN` | `10` | janela do marcador de idempotência |
| `TRACKING_ERRO_TENTATIVAS` | `2` | reentregas antes do dead letter |
| `TRACKING_ERRO_BACKOFF_MS` | `500` | espera entre reentregas |
| `REDIS_HOST` | `localhost` | read model e pub/sub |
| `REDIS_PORT` | `6379` | porta do Redis |
