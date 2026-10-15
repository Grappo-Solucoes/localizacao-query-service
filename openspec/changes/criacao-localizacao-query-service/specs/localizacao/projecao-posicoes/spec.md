# Spec Delta

## Purpose

Mantém o read model de posições de veículo e de aluno a partir dos eventos publicados no Kafka, para que o lado query responda às consultas sem depender do Firebase nem de qualquer outra fonte de rastreamento.

## ADDED Requirements

### Requirement: Consumo das posições de veículo

O serviço SHALL consumir as posições de veículo do tópico `tracking.posicao-veiculo.v1`, cujo payload identifica a viagem por `viagemId`, e SHALL usar apenas os campos publicados nesse contrato: `eventId`, `viagemId`, `latitude`, `longitude`, `timestamp`, `bateria`, `precisao` e `provedor`.

#### Scenario: Posição de veículo projetada
- **WHEN** chega um evento válido de posição de veículo
- **THEN** a posição consultável daquele `viagemId` passa a ser a do evento, com o `timestamp` do evento

#### Scenario: Campos opcionais ausentes
- **WHEN** o evento chega sem `bateria`, `precisao` ou `provedor`
- **THEN** a projeção conclui normalmente, com esses atributos vazios

### Requirement: Consumo das posições de aluno
O serviço SHALL consumir as posições de aluno do tópico `tracking.posicao-aluno.v1`, cujo payload identifica o aluno por `alunoId` e a viagem por `viagemId`, e SHALL manter a posição consultável do aluno associada à viagem informada no evento.

#### Scenario: Posição de aluno projetada
- **WHEN** chega um evento válido de posição de aluno
- **THEN** a posição consultável daquele `alunoId` passa a ser a do evento e fica associada ao `viagemId` do evento

### Requirement: Idempotência por eventId
O consumo é at-least-once: o serviço SHALL aplicar cada `eventId` no máximo uma vez dentro da janela de idempotência, para que reentrega de registro ou rebalanceamento de partição não apliquem a mesma posição duas vezes.

#### Scenario: Reentrega do mesmo evento
- **WHEN** um `eventId` já processado é entregue novamente
- **THEN** a posição consultável permanece a mesma e nenhum efeito é aplicado duas vezes

#### Scenario: Falha depois de o evento ser marcado
- **WHEN** a projeção falha depois de o evento ter sido marcado como processado
- **THEN** o marcador de idempotência é liberado, de modo que a reentrega consiga processar o evento

### Requirement: Descarte de evento fora do contrato
O serviço SHALL descartar, e confirmar, o evento que não identifica a entidade — `eventId` ausente ou em branco, ou `viagemId`/`alunoId` ausente ou em branco — e o evento cujo `timestamp` não seja positivo, porque reentregá-lo não o tornaria válido.

#### Scenario: Evento sem identificação
- **WHEN** o evento chega sem `eventId`, ou sem `viagemId`/`alunoId`
- **THEN** o evento é descartado, nada é projetado e o deslocamento avança

#### Scenario: Timestamp inválido
- **WHEN** o evento chega com `timestamp` igual a zero ou negativo
- **THEN** o evento é descartado, nada é projetado e o deslocamento avança

### Requirement: Confirmação só depois da projeção
O serviço SHALL confirmar o deslocamento somente depois de a projeção concluir com sucesso e SHALL manter o registro elegível para reentrega quando a projeção falhar.

#### Scenario: Falha na projeção
- **WHEN** a projeção de um evento falha
- **THEN** o deslocamento daquele registro não é confirmado e o evento permanece elegível para reentrega

### Requirement: Retentativa limitada e dead letter
O serviço SHALL retentar um registro que falha por um número limitado de vezes e, esgotadas as tentativas, SHALL encaminhá-lo a um destino de dead letter próprio do tópico, para que um registro envenenado não bloqueie a partição.

#### Scenario: Registro envenenado
- **WHEN** um registro continua falhando depois das tentativas configuradas
- **THEN** ele é publicado no destino de dead letter daquele tópico e o deslocamento avança
