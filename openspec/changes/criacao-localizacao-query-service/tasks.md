# Tasks

## 1. Entrada Kafka

- [x] 1.1 Adicionar `spring-kafka` e as propriedades de consumo (`bootstrap-servers`, os dois tópicos, `group-id` próprio e diferente de `localizacao-core`, `auto-offset-reset=latest`, `ack-mode=manual_immediate`) — verificação: `./mvnw -q -DskipTests package` conclui e as chaves estão listadas em `application.properties`
- [x] 1.2 Implementar os listeners dos tópicos `tracking.posicao-veiculo.v1` e `tracking.posicao-aluno.v1` com DTOs do contrato v1 (`eventId`, `viagemId`, `alunoId`, `latitude`, `longitude`, `timestamp`, `bateria`, `precisao`, `provedor`, `occurredOn`) e tolerância a campos desconhecidos — verificação: teste unitário desserializa o payload real dos dois tópicos e chama a projeção
- [x] 1.3 Descartar e confirmar o evento fora do contrato: sem `eventId`, sem `viagemId`/`alunoId` ou com `timestamp` não positivo — verificação: testes cobrem cada caso e comprovam que nada foi projetado e que a confirmação aconteceu
- [x] 1.4 Aplicar idempotência por `eventId` com marcador no Redis (TTL explícito) e liberar o marcador quando a projeção falha — verificação: teste de reentrega não reprojeta, e teste de falha comprova que o marcador foi liberado e a reentrega processa
- [x] 1.5 Confirmar o offset somente após a projeção e encaminhar registro que falha, esgotadas as tentativas, para o dead letter do próprio tópico — verificação: teste de integração com Kafka real publica registro envenenado e confere a mensagem no dead letter e o offset avançado
- [x] 1.6 Remover `LocationConsumer`, `RabbitConfig` e os bindings de `location.exchange`, e retirar do `pom.xml` `spring-boot-starter-amqp`, `spring-cloud-stream` e `spring-cloud-stream-binder-rabbit` — verificação: `./mvnw -q -DskipTests package` conclui e `./mvnw -q dependency:tree` não lista amqp nem spring-cloud-stream
- [x] 1.7 Documentar no README o contrato consumido (tópicos, chaves, campos, grupo de consumo) e como subir o serviço com um broker local — verificação: os comandos escritos no README executam como estão documentados

## 2. Read model

- [x] 2.1 Projetar a posição de veículo e de aluno nas views existentes usando apenas os campos do contrato v1 e manter a associação da posição do aluno à sua viagem — verificação: teste grava e lê a view das duas entidades conferindo coordenadas, `timestamp` e `viagemId` do aluno
- [x] 2.2 Gravar o geohash de 8 caracteres em cada posição projetada — verificação: teste confere comprimento e valor do geohash para uma coordenada conhecida
- [x] 2.3 Aplicar uma janela única e configurável de expiração do read model, dimensionada acima do intervalo esperado entre publicações — verificação: teste com janela curta comprova presença dentro da janela e ausência reportada depois dela
- [x] 2.4 Remover das views os atributos que o contrato Kafka não preenche e registrar a consequência no README — verificação: inspeção das views sem os campos enriquecidos e README descrevendo o efeito sobre as respostas

## 3. Consulta REST

- [x] 3.1 Expor `GET /api/posicoes/viagem/{viagemId}` devolvendo coordenadas e `timestamp` da última posição, e ausência explícita quando não houver posição válida — verificação: teste de controller cobre os dois desfechos
- [x] 3.2 Expor `GET /api/posicoes/aluno/{alunoId}` devolvendo coordenadas, `timestamp` e o `viagemId` associado, e ausência explícita quando não houver posição válida — verificação: teste de controller cobre os dois desfechos
- [x] 3.3 Documentar os dois endpoints no README, incluindo o desfecho de ausência — verificação: a requisição documentada reproduz o resultado descrito

## 4. Acompanhamento em tempo real

- [x] 4.1 Sanear o handler do WebSocket: remover o canal da viagem quando a conexão encerra e extrair o identificador do caminho com validação, recusando formato inválido — verificação: teste conecta e desconecta comprovando que o mapa volta a ficar vazio, e teste de caminho inválido comprova a recusa
- [x] 4.2 Garantir entrega apenas aos clientes que acompanham aquela viagem — verificação: teste com dois clientes em viagens distintas comprova que cada um recebe só a sua
- [x] 4.3 Manter a entrega entre instâncias via pub/sub do Redis, de modo que a projeção feita em uma instância chegue ao cliente conectado a outra — verificação: teste com dois handlers comprova que a publicação em um alcança o cliente do outro
- [x] 4.4 Manter a métrica de conexões exposta de forma coletável — verificação: teste confere o valor após abrir e fechar conexões

## 5. Integração e operação

- [x] 5.1 Teste de ponta a ponta com Kafka e Redis reais: publicar posição de veículo e de aluno e conferir read model, respostas REST e push — verificação: execução do grupo de integração passa sem falhas
- [x] 5.2 Conferir que a fila `location.query.queue` e os bindings de `location.exchange` deixaram de ser declarados e que nada mais depende deles — verificação: com o broker no ar, a listagem de filas e bindings não mostra `location.query.queue` nem seus bindings
- [x] 5.3 Conferir a subida do serviço com as variáveis de ambiente documentadas — verificação: imagem construída pelo `Dockerfile` sobe contra broker e Redis locais e `GET /api/posicoes/viagem/{id}` responde conforme a spec

## Workflow follow-up

- Arquivar o change depois da revisão.
- Conferir o resultado do arquivamento (specs principais geradas para as duas capabilities).
