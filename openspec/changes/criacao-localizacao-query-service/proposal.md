# Proposal

## Why

O mapa da operação (Visão ao Vivo) ainda é servido pelo monólito `busco-api`, que monta o snapshot lendo a posição do veículo direto do Firebase RTDB a cada ciclo de 5 s; o monólito não tem tabela de posição nem mensageria. A migração já tirou a ingestão do Firebase — `tracking-ingestion-service` lê o RTDB e publica as posições no Kafka — e o `localizacao-core-service` já consome esses tópicos, mas ele é o lado **command**: publica um snapshot STOMP vazio (`VisaoGeralScheduler` monta `Painel.of(0,0,0)` com lista vazia) e não expõe nenhuma leitura.

O repositório do lado query já existe (`localizacao-query-service`, commit de 2026-02-24) com o arcabouço de projeção, read model em Redis, geohash e push em tempo real — porém ligado a outra entrada (fila Rabbit do `location.exchange`), sem consulta servida, sem testes e sem documentação. Este change liga o serviço à entrada correta (Kafka) e o torna de fato consultável.

## What Changes

**Entrada — BREAKING**
- Passa a consumir os tópicos Kafka `tracking.posicao-veiculo.v1` (chave `viagemId`) e `tracking.posicao-aluno.v1` (chave `alunoId`), publicados pelo `tracking-ingestion-service`, com commit manual de offset e idempotência por `eventId`.
- **BREAKING**: sai o consumo da fila Rabbit `location.query.queue` (bindings `viagem.posicao` e `aluno.posicao` do `location.exchange`) e, com ele, os campos que só existiam no evento enriquecido do core: `velocidade`, `endereco`, `distanciaParaDestino`, `etaSegundos`, `status` e `proximoPonto*`. O contrato v1 do Kafka carrega apenas `eventId`, `viagemId`, `latitude`, `longitude`, `timestamp`, `bateria`, `precisao`, `provedor` e `occurredOn` (no tópico de aluno, sem `bateria`/`precisao`/`provedor`). Esses campos deixam de ser projetados.

**Read model**
- View da viagem e view do aluno em Redis, consultáveis por `viagemId` e `alunoId`, com semântica de expiração explícita — hoje 10 s para a viagem e 30 s para o aluno, o que apaga a viagem do read model numa pausa de publicação.
- Geohash de 8 caracteres por posição, para consulta por região.

**Consulta**
- Endpoints REST de leitura sobre o read model.
- Push em tempo real por viagem, com isolamento por conexão e sem recursos vazados.

**Robustez e qualidade**
- Correções no código existente: `Sinks` de room nunca removidos do mapa, `path.split("/")[3]` frágil no handshake, `@ConditionalOnExpression` sem efeito sobre `@RabbitHandler`.
- Descarte de payload fora do contrato e política de erro/retentativa no consumo.
- README com o contrato e como subir, e a suíte de testes — hoje inexistentes.
- Não-objetivo: substituir o mapa do `busco-api` (contrato `VisaoGeral{Painel,Detalhes}` em STOMP `/topic/visao-geral`). O `busco-web` continua apontando para o monólito até um change seguinte.

## Capabilities

### New Capabilities
- `localizacao/projecao-posicoes`: consumo dos eventos de posição de veículo e de aluno e manutenção do read model consultável, incluindo idempotência, descarte de payload fora do contrato e política de erro.
- `localizacao/consulta-posicoes`: leitura do read model — consulta REST e push em tempo real por viagem — e a semântica de expiração e de ausência de dado.

### Modified Capabilities
Nenhuma: o projeto ainda não tem specs (`openspec list --specs` retorna vazio), então todas as capabilities deste change são novas.

## Impact

- **Repo** `localizacao-query-service`: `app/LocationProjectionService`, `app/TrackingEvent`, `app/geo/GeoHashService`, `config/{RabbitConfig,RedisConfig,RedisReactiveConfig,WebSocketConfig}`, `domain/{ViagemTrackingView,AlunoTrackingView}`, `domain/events/*`, `infra/rabbit/LocationConsumer`, `infra/redis/*`, `ui/rest/WebSocketMetricsController`, `websocket/{RoomPublisher,RoomSocketHandler}`.
- **Build**: `pom.xml` passa a precisar de `spring-kafka`; saem `spring-cloud-stream` e `spring-cloud-stream-binder-rabbit` (declarados e sem uso) e `spring-boot-starter-amqp`, se o Rabbit for de fato removido. `factual:geo` (repo `clojars`) continua sendo a origem do geohash.
- **Mensageria**: entram os dois tópicos Kafka e o grupo de consumo do serviço; saem `location.query.queue` e os bindings de `location.exchange` (o exchange permanece com o `localizacao-core-service`).
- **Redis**: chaves `tracking:viagem:{viagemId}` e `tracking:aluno:{alunoId}`; canal pub/sub `tracking.viagem.{viagemId}`.
- **Consumidores**: `busco-web` e `busco-app` não são alterados por este change.
- **Deploy**: `.github/workflows/prd.yaml` já publica a imagem e atualiza `deployment/localizacao-query-service`; passam a ser necessárias as variáveis de Kafka no cluster, e o broker Kafka ainda não tem manifesto neste workspace.
