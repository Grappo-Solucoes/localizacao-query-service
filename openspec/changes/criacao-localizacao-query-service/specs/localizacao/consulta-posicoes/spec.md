# Spec Delta

## Purpose

Expõe as posições projetadas para consulta — leitura da última posição de uma viagem ou de um aluno e acompanhamento em tempo real por viagem — para que os clientes da operação não dependam da fonte original de rastreamento.

## ADDED Requirements

### Requirement: Consulta da última posição de uma viagem
O serviço SHALL oferecer uma operação de leitura que devolve a última posição projetada de uma viagem identificada por `viagemId`.

#### Scenario: Posição disponível
- **WHEN** a viagem tem posição projetada
- **THEN** a resposta traz latitude, longitude e o `timestamp` da posição

#### Scenario: Posição indisponível
- **WHEN** não há posição projetada para a viagem, por nunca ter sido recebida ou por ter expirado
- **THEN** a resposta sinaliza explicitamente a ausência e não devolve coordenadas

### Requirement: Consulta da última posição de um aluno
O serviço SHALL oferecer uma operação de leitura que devolve a última posição projetada de um aluno identificado por `alunoId`, junto da viagem à qual essa posição está associada.

#### Scenario: Posição disponível
- **WHEN** o aluno tem posição projetada
- **THEN** a resposta traz latitude, longitude, o `timestamp` da posição e o `viagemId` associado

#### Scenario: Posição indisponível
- **WHEN** não há posição projetada para o aluno, por nunca ter sido recebida ou por ter expirado
- **THEN** a resposta sinaliza explicitamente a ausência e não devolve coordenadas

### Requirement: Acompanhamento em tempo real por viagem
O serviço SHALL permitir que um cliente acompanhe uma viagem por uma conexão persistente e receba cada nova posição projetada daquela viagem.

#### Scenario: Nova posição com clientes acompanhando
- **WHEN** uma nova posição é projetada para uma viagem que tem clientes acompanhando
- **THEN** esses clientes recebem a atualização

#### Scenario: Isolamento entre viagens
- **WHEN** um cliente está acompanhando a viagem A
- **THEN** ele não recebe atualizações de posições de outras viagens

### Requirement: Entrega entre instâncias
O serviço SHALL entregar a atualização a qualquer cliente que acompanhe a viagem, independentemente de qual instância projetou a posição.

#### Scenario: Projeção em outra instância
- **WHEN** a posição é projetada por uma instância e o cliente está conectado a outra instância
- **THEN** o cliente recebe a atualização

### Requirement: Expiração do read model
O serviço SHALL considerar a posição projetada válida por uma janela de tempo limitada sem novas posições daquela entidade e, passada a janela, SHALL passar a reportar ausência para as consultas daquela entidade.

#### Scenario: Janela expirada
- **WHEN** não chega posição nova para a entidade dentro da janela
- **THEN** a consulta daquela entidade passa a reportar ausência

#### Scenario: Nova posição dentro da janela
- **WHEN** chega posição nova para a entidade antes de a janela terminar
- **THEN** a entidade continua consultável, com a posição mais recente

### Requirement: Métrica de conexões
O serviço SHALL expor, de forma coletável, a quantidade atual de conexões persistentes de acompanhamento.

#### Scenario: Coleta da métrica
- **WHEN** o coletor consulta a métrica de conexões
- **THEN** recebe o número de conexões abertas no momento
