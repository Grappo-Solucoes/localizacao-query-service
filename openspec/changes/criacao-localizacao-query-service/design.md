# Design

## Context

O repositório já contém o arcabouço do lado query (28 arquivos no `main`, último commit 2026-02-24) e é ele que este change evolui — ver `proposal.md` para a motivação.

Estado atual do código, como ele é hoje:

- Entrada: `LocationConsumer` em `location.query.queue`, com bindings `viagem.posicao` e `aluno.posicao` no `location.exchange` e handlers por `@RabbitHandler`.
- Projeção: `LocationProjectionService` grava `ViagemTrackingView` (`tracking:viagem:{viagemId}`, TTL 10 s) e `AlunoTrackingView` (`tracking:aluno:{alunoId}`, TTL 30 s) em Redis, e publica em pub/sub no canal `tracking.viagem.{viagemId}`.
- Leitura: WebSocket cru do WebFlux em `/ws/viagem/{viagemId}` com rooms em `Sinks` mantidos em memória; `RedisPubSubSubscriber` faz a ponte pub/sub → rooms. `GET /metrics/connections` devolve a contagem de conexões em texto estilo Prometheus.
- Geohash de 8 caracteres por posição via `factual:geo` (repositório `clojars`).
- `pom.xml`: Spring Boot 3.3.2, Java 17, `spring-boot-starter-amqp`, `data-redis-reactive`, `webflux`, `spring-cloud-stream` + `spring-cloud-stream-binder-rabbit`; sem `spring-kafka`, sem actuator, sem springdoc, sem JPA.
- Zero testes (`src/test` não existe) e sem README. `spring.data.redis.host=redis` está fixo no `application.properties`.

Contratos externos que a implementação precisa respeitar:

- Produtor: `tracking-ingestion-service` publica `tracking.posicao-veiculo.v1` (chave `viagemId`) e `tracking.posicao-aluno.v1` (chave `alunoId`) — JSON sem type headers, 3 partições, 1 réplica, `acks=all`, produtor idempotente. No veículo: `eventId`, `viagemId`, `latitude`, `longitude`, `timestamp` (relógio do aparelho, epoch ms), `bateria`, `precisao`, `provedor`, `occurredOn` (relógio do ingestor). No aluno: os mesmos campos sem `bateria`, `precisao` e `provedor`. O app não grava `provider`, então `provedor` chega nulo.
- Consumidor irmão: `localizacao-core-service` (PR empilhado) já consome os dois tópicos com `groupId=localizacao-core`, `ack-mode=manual_immediate`, `auto-offset-reset=latest`, idempotência em `evento:processado:{eventId}` (TTL 10 min) e DLT por tópico com `FixedBackOff(500 ms, 2)`. É o precedente que este serviço espelha.
- Os campos enriquecidos que a view tem hoje (`velocidade`, `endereco`, `distanciaParaDestino`, `etaSegundos`, `status`, `proximoPonto*`) só existem no evento Rabbit do core; o contrato Kafka v1 não os carrega.

Deploy: `.github/workflows/prd.yaml` builda, publica no ACR e executa `kubectl set image deployment/localizacao-query-service`. Não existe manifesto de Kafka em nenhum repositório do workspace.

## Goals / Non-Goals

**Goals:**

- Ligar o serviço aos tópicos Kafka do ingestor e manter o read model de viagem e de aluno consultável.
- Expor a consulta: leitura REST por `viagemId` e por `alunoId`, e acompanhamento em tempo real por viagem com entrega entre instâncias.
- Reaproveitar o que existe (projeção, views, geohash, pub/sub, rooms), trocando a entrada em vez de reescrever o serviço.
- Robustez de consumo: confirmação manual, idempotência, descarte de payload fora do contrato e dead letter.

**Non-Goals:**

- Servir o mapa do `busco-web` (contrato `VisaoGeral{Painel,Detalhes}` em STOMP `/topic/visao-geral`) — change seguinte.
- Repor os campos enriquecidos que só o evento Rabbit carrega; se voltarem a ser necessários, a decisão é do lado que publica.
- Autenticação e autorização das conexões e dos endpoints de consulta.
- Persistência durável (Postgres/Flyway) de histórico de posições: o read model continua efêmero.

## Decisions

**D1 — Entrada passa a ser Kafka, não a fila Rabbit do `location.exchange`.**
O serviço é a contraparte query da topologia alvo (Kafka → `localizacao-query`) e o pedido foi explícito quanto a consumir os eventos do Kafka. Alternativas: (a) manter o Rabbit consumindo os eventos enriquecidos do core, que preservaria `velocidade`/`etaSegundos`/`proximoPonto*` mas acopla o lado query ao command e contradiz a topologia; (b) híbrido Kafka para posição + Rabbit para enriquecimento, que dobra os caminhos de entrada e a superfície de consistência sem necessidade demonstrada.

**D2 — Grupo de consumo próprio, distinto do core.**
`spring.kafka.consumer.group-id` do serviço MUST ser diferente de `localizacao-core`. Com o mesmo grupo, os dois serviços dividiriam as partições e cada um receberia metade das posições — o core perderia eventos do seu pipeline e este serviço perderia posições do read model.

**D3 — Idempotência por `eventId` em Redis, com janela explícita e liberação em falha.**
Espelha o core (`evento:processado:{eventId}`): a chave é tomada com `SET NX` + TTL antes de processar, e é liberada quando o processamento falha. A liberação é o que permite que a reentrega aconteça e que o dead letter seja acionado — sem ela a falha fica silenciosa e o registro nunca chega ao DLT (defeito já encontrado e corrigido no core). Alternativas: dedupe por `(entidadeId, timestamp)` — não cobre reentrega depois de o read model expirar; idempotência em banco — exige uma persistência que o serviço não tem.

**D4 — Confirmação do offset só depois da projeção; erro vai para `<topico>.DLT`.**
`ack-mode=manual`, com `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` e backoff fixo limitado (mesmo desenho do core). Alternativas: `ack` automático, que perde a posição numa falha; retentativa infinita, que trava a partição num registro envenenado. `auto-offset-reset=latest`: o read model só interessa a partir de agora, então reprocessar o histórico na primeira subida não é desejado.

**D5 — Read model continua em Redis, com uma janela de expiração única e explícita.**
Hoje são 10 s para a viagem e 30 s para o aluno. Como o ingestor só publica quando chega posição nova, uma janela de 10 s apaga a viagem do read model num silêncio normal de publicação — o veículo some do acompanhamento sem nada ter acontecido. A janela passa a ser uma só, configurável, dimensionada acima do intervalo esperado entre publicações. Alternativas: sem expiração, que faz o read model crescer sem limite e serve posição antiga como se fosse atual; TTL longo único fixo, que simplifica mas serve dado obsoleto por mais tempo.

**D6 — Entrega em tempo real mantém o desenho projeção → pub/sub → rooms.**
O grupo de consumo entrega cada posição a **uma** instância; sem a ponte de pub/sub no Redis, um cliente conectado a outra instância não receberia nada. É a forma mais barata de manter fan-out correto com múltiplas réplicas. Alternativas: afinidade de sessão (sticky) por instância — exige configuração de ingress e quebra em rollout; broker STOMP externo — troca dependência por mais infraestrutura.

**D7 — Superfície de consulta: `/api/posicoes/...` + o WebSocket atual.**
`GET /api/posicoes/viagem/{viagemId}` e `GET /api/posicoes/aluno/{alunoId}` devolvem a última posição projetada e sinalizam ausência com `404` quando não há posição válida. O caminho acompanha a convenção `/api/<recurso>` dos serviços irmãos, sem repetir `/api/viagens/{viagemId}/...`, que já pertence ao `localizacao-core-service` e colidiria no roteamento do gateway. O WebSocket continua em `/ws/viagem/{viagemId}` e a métrica de conexões continua em `/metrics/connections`. Alternativas: `/api/viagens/{id}/posicao` — ambiguidade de rota entre dois serviços; migrar para STOMP `/topic/visao-geral` agora — mistura este change com o do mapa.

**D8 — Limpeza do que sobra da via Rabbit.**
Saem `spring-boot-starter-amqp`, `spring-cloud-stream` e `spring-cloud-stream-binder-rabbit` (o consumo é por `@RabbitListener`, não por bindings funcionais, então o `spring-cloud-stream` declarado nunca foi usado), junto do `RabbitConfig` e do `LocationConsumer`. Com isso sai também o `@ConditionalOnExpression` sobre `@RabbitHandler`, que não tem efeito — a seleção do handler é pelo tipo do payload. Entra `spring-kafka`. O geohash continua vindo de `ch.hsr:geohash`, declarado direto: a dependência `factual:geo` que estava no `pom.xml` é uma biblioteca Clojure sem uso no código e só entrava por ela o `ch.hsr:geohash` transitivo, ao custo de um repositório Clojars a mais no build.

**D9 — Defeitos conhecidos que entram junto por estarem na superfície tocada.**
O `Sink` de cada room nunca é removido do mapa em `RoomSocketHandler` (vazamento por conexão encerrada), e o `viagemId` é extraído com `path.split("/")[3]` sem validação de formato. Ambos serão saneados junto das tasks de consulta.

## Risks / Trade-offs

- [Perda dos campos enriquecidos] → o read model e a resposta de consulta passam a expor apenas o que o contrato v1 carrega (coordenadas, `timestamp`, `bateria`, `precisao`, `provedor`, `geohash`); `velocidade`, `endereco`, `status` e `proximoPonto*` ficam vazios nas views. Fica documentado no README, e a decisão de voltar a publicá-los é do lado produtor.
- [Janela de expiração curta apaga a viagem em silêncio] → janela única, configurável e dimensionada acima do intervalo esperado entre publicações; a consulta sinaliza ausência em vez de devolver posição vencida.
- [Vazamento de memória nos rooms] → remover o `Sink` do mapa quando a conexão encerra; cenário de teste conecta, desconecta e confere que o mapa volta a ficar vazio.
- [Handshake fora do padrão] → extrair o identificador do path com validação explícita e recusar o handshake fora do formato, em vez de estourar índice.
- [Kafka inexistente no cluster] → sem broker o serviço sobe e não projeta nada; provisioná-lo é pré-requisito de deploy, e hoje só existe `docker-compose.dev.yml` no repositório irmão.
- [Ordem por entidade depende da chave da mensagem] → a última posição só é "a última" porque a chave é `viagemId`/`alunoId`; mudar a chave do produtor quebra a premissa e precisa ser tratado do lado de quem publica.
- [Sem idempotência de entrega ao cliente] → uma reentrega pode repetir a atualização para o cliente conectado; aceitável enquanto o cliente trata atualização de posição como idempotente.

## Migration Plan

1. Trocar a entrada para os tópicos Kafka mantendo projeção, views, pub/sub e rooms; `location.query.queue` e os bindings de `location.exchange` deixam de ser declarados (o exchange continua pertencendo ao `localizacao-core-service`).
2. Ajustar `pom.xml` e as propriedades do serviço (bootstrap, tópicos, grupo, `ack-mode`, janela de expiração, idempotência).
3. Rollback: reverter o commit devolve o consumo Rabbit anterior. Não há dado durável a migrar — o read model é efêmero e se repovoa nas primeiras posições após a subida.
4. Deploy pela pipeline existente (`prd.yaml` → ACR → `kubectl set image`), condicionado às variáveis de Kafka no cluster.

## Open Questions

- Valor final da janela de expiração do read model: entra como configuração com default documentado; o ajuste fino depende do intervalo real de publicação em campo.
- Momento de repor os campos enriquecidos (`velocidade`, `endereco`, `status`, `proximoPonto*`) na posição de origem — não altera as specs, a abordagem nem as tasks deste change.
