package com.busco.localizacao;

import com.busco.localizacao.app.PosicaoPublisher;
import com.busco.localizacao.domain.ViagemTrackingView;
import com.busco.localizacao.domain.events.PosicaoAlunoRegistrada;
import com.busco.localizacao.domain.events.PosicaoVeiculoRegistrada;
import com.busco.localizacao.domain.repository.EventoProcessadoRepository;
import com.busco.localizacao.infra.redis.RedisPubSubSubscriber;
import com.busco.localizacao.support.EventoProcessadoEmMemoria;
import com.busco.localizacao.support.PosicaoReadModelEmMemoria;
import com.busco.localizacao.websocket.SalasDeAcompanhamento;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ponta a ponta: posicao publicada no Kafka chega ao read model, a consulta REST responde e o
 * cliente que acompanha a viagem recebe o push.
 *
 * <p>O broker e o embutido do {@code spring-kafka-test}. Sem Redis neste ambiente, o read model e o
 * marcador sao dublês em memoria e o salto do pub/sub e simulado pelo publicador de teste, que
 * entrega o mesmo JSON que o Redis entregaria as conexoes locais.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.listener.auto-startup=true",
        "tracking.posicao.ttl-segundos=120"
})
@EmbeddedKafka(partitions = 1, topics = {PipelineQueryE2ETest.TOPICO_VEICULO, PipelineQueryE2ETest.TOPICO_ALUNO})
@AutoConfigureWebTestClient
class PipelineQueryE2ETest {

    static final String TOPICO_VEICULO = "tracking.posicao-veiculo.v1";
    static final String TOPICO_ALUNO = "tracking.posicao-aluno.v1";

    private static final String VIAGEM = "viagem-e2e";
    private static final String ALUNO = "aluno-e2e";
    private static final Duration ESPERA = Duration.ofSeconds(30);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private PosicaoReadModelEmMemoria readModel;

    @Autowired
    private org.springframework.core.env.Environment ambiente;

    @Autowired
    private SalasDeAcompanhamento salas;

    @MockBean
    private ReactiveRedisMessageListenerContainer container;

    @MockBean
    private RedisPubSubSubscriber redisPubSubSubscriber;

    @Test
    void posicaoNoKafkaChegaAoReadModelNaConsultaENoAcompanhamento() throws Exception {
        BlockingQueue<String> recebidasPeloCliente = acompanhar(VIAGEM);

        kafkaTemplate.send(TOPICO_VEICULO, VIAGEM, new PosicaoVeiculoRegistrada(UUID.randomUUID().toString(),
                VIAGEM, -23.5505, -46.6333, 1_700_000_000_000L, 88.5, 12.5, "gps", Instant.now().toString()))
                .get(10, TimeUnit.SECONDS);

        aguardar(() -> readModel.viagem(VIAGEM) != null, "a posicao do veiculo deveria chegar ao read model");

        webTestClient.get().uri("/api/posicoes/viagem/{viagemId}", VIAGEM)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.viagemId").isEqualTo(VIAGEM)
                .jsonPath("$.latitude").isEqualTo(-23.5505)
                .jsonPath("$.longitude").isEqualTo(-46.6333)
                .jsonPath("$.timestamp").isEqualTo(1_700_000_000_000L);

        String recebida = recebidasPeloCliente.poll(ESPERA.toSeconds(), TimeUnit.SECONDS);
        assertNotNull(recebida, "o cliente que acompanha a viagem deveria receber o push");
        assertTrue(recebida.contains(VIAGEM) && recebida.contains("-23.5505"),
                "o push deveria carregar a posicao projetada: " + recebida);
    }

    @Test
    void posicaoDeAlunoNoKafkaFicaConsultaVeLAssociadaAViagem() throws Exception {
        kafkaTemplate.send(TOPICO_ALUNO, ALUNO, new PosicaoAlunoRegistrada(UUID.randomUUID().toString(),
                VIAGEM, ALUNO, -23.5505, -46.6333, 1_700_000_000_000L, Instant.now().toString()))
                .get(10, TimeUnit.SECONDS);

        aguardar(() -> readModel.aluno(ALUNO) != null, "a posicao do aluno deveria chegar ao read model");

        webTestClient.get().uri("/api/posicoes/aluno/{alunoId}", ALUNO)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.alunoId").isEqualTo(ALUNO)
                .jsonPath("$.viagemId").isEqualTo(VIAGEM)
                .jsonPath("$.latitude").isEqualTo(-23.5505);
    }

    @Test
    void consultaDeViagemSemPosicaoRespondeAusencia() {
        webTestClient.get().uri("/api/posicoes/viagem/{viagemId}", "viagem-sem-posicao")
                .exchange()
                .expectStatus().isNotFound();
    }

    private BlockingQueue<String> acompanhar(String viagemId) throws InterruptedException {
        BlockingQueue<String> recebidas = new LinkedBlockingQueue<>();
        String porta = ambiente.getProperty("local.server.port");

        new ReactorNettyWebSocketClient()
                .execute(URI.create("ws://localhost:" + porta + "/ws/viagem/" + viagemId), sessao ->
                        sessao.receive()
                                .map(WebSocketMessage::getPayloadAsText)
                                .doOnNext(recebidas::add)
                                .then())
                .subscribe();

        // Sem a conexao registrada na sala, o push nao teria para quem ir.
        aguardar(() -> salas.conexoes() > 0, "a conexao do cliente deveria entrar na sala da viagem");

        return recebidas;
    }

    private void aguardar(java.util.function.BooleanSupplier condicao, String mensagem)
            throws InterruptedException {
        long limite = System.nanoTime() + ESPERA.toNanos();
        while (System.nanoTime() < limite) {
            if (condicao.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError(mensagem);
    }

    /** Colaboradores sem infraestrutura, como no teste de dead letter. */
    @TestConfiguration
    static class ColaboradoresEmMemoria {

        @Bean
        @Primary
        PosicaoReadModelEmMemoria readModel() {
            return new PosicaoReadModelEmMemoria();
        }

        @Bean
        @Primary
        EventoProcessadoRepository marcador() {
            return new EventoProcessadoEmMemoria();
        }

        /**
         * Simula o salto pelo Redis: serializa o payload como o publicador de producao e entrega nas
         * conexoes locais pelo mesmo canal.
         */
        @Bean
        @Primary
        PosicaoPublisher publisher(ObjectMapper mapper, SalasDeAcompanhamento salas) {
            return (viagemId, payload) -> {
                try {
                    return salas.publicar(viagemId, mapper.writeValueAsString(payload));
                } catch (Exception e) {
                    return Mono.error(e);
                }
            };
        }
    }
}
