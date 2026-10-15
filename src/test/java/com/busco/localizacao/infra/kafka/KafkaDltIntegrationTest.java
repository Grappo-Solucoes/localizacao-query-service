package com.busco.localizacao.infra.kafka;

import com.busco.localizacao.app.PosicaoPublisher;
import com.busco.localizacao.domain.ViagemTrackingView;
import com.busco.localizacao.domain.events.PosicaoVeiculoRegistrada;
import com.busco.localizacao.domain.repository.EventoProcessadoRepository;
import com.busco.localizacao.infra.redis.RedisPubSubSubscriber;
import com.busco.localizacao.support.EventoProcessadoEmMemoria;
import com.busco.localizacao.support.PosicaoReadModelEmMemoria;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova, com broker real, que uma posicao que falha na projecao nao trava a particao: depois da
 * retentativa limitada o registro vai para {@code <topico>.DLT} e o deslocamento avanca.
 *
 * <p>O broker e o embutido do {@code spring-kafka-test}, criado na propria JVM. Nao ha Redis neste
 * ambiente: o read model e o marcador de idempotencia sao dublês em memoria e os beans do
 * pub/sub ficam desligados, porque a conexao do listener de Redis acontece no startup.</p>
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.listener.auto-startup=true",
        "tracking.erro.tentativas=1",
        "tracking.erro.backoff-ms=100"
})
@EmbeddedKafka(partitions = 1, topics = {KafkaDltIntegrationTest.TOPICO, KafkaDltIntegrationTest.TOPICO + ".DLT"})
class KafkaDltIntegrationTest {

    static final String TOPICO = "tracking.posicao-veiculo.v1";
    private static final String VIAGEM_VENENOSA = "viagem-dlt";
    private static final String VIAGEM_SEGUINTE = "viagem-ok";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PosicaoReadModelEmMemoria readModel;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @MockBean
    private ReactiveRedisMessageListenerContainer container;

    @MockBean
    private RedisPubSubSubscriber redisPubSubSubscriber;

    @Test
    void registroQueFalhaNaProjecaoVaiParaDeadLetterEAvancaOOffset() throws Exception {
        PosicaoVeiculoRegistrada venenoso = new PosicaoVeiculoRegistrada(UUID.randomUUID().toString(),
                VIAGEM_VENENOSA, -23.5505, -46.6333, 1_700_000_000_000L, null, null, null, Instant.now().toString());
        kafkaTemplate.send(TOPICO, venenoso.viagemId(), venenoso).get(10, TimeUnit.SECONDS);

        String corpo = consumirDlt(TOPICO + ".DLT", venenoso.eventId(), Duration.ofSeconds(30));

        assertNotNull(corpo, "nenhum registro com o eventId esperado chegou ao topico de dead letter");
        assertTrue(corpo.contains(VIAGEM_VENENOSA), "o registro no DLT deveria carregar o evento original: " + corpo);

        // O deslocamento avancou: o registro seguinte da mesma particao e projetado.
        PosicaoVeiculoRegistrada seguinte = new PosicaoVeiculoRegistrada(UUID.randomUUID().toString(),
                VIAGEM_SEGUINTE, -23.5505, -46.6333, 1_700_000_009_000L, null, null, null, Instant.now().toString());
        kafkaTemplate.send(TOPICO, seguinte.viagemId(), seguinte).get(10, TimeUnit.SECONDS);

        assertTrue(aguardarProjecaoDe(VIAGEM_SEGUINTE, Duration.ofSeconds(30)),
                "o registro seguinte deveria ser projetado, provando que a particao nao ficou presa");
    }

    private boolean aguardarProjecaoDe(String viagemId, Duration tempoLimite) throws InterruptedException {
        long limite = System.nanoTime() + tempoLimite.toNanos();
        while (System.nanoTime() < limite) {
            if (readModel.viagem(viagemId) != null) {
                return true;
            }
            Thread.sleep(200);
        }
        return false;
    }

    private String consumirDlt(String topico, String eventIdEsperado, Duration tempoLimite) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topico));
            long limite = System.nanoTime() + tempoLimite.toNanos();
            while (System.nanoTime() < limite) {
                for (var registro : consumer.poll(Duration.ofMillis(500))) {
                    if (registro.value() != null && registro.value().contains(eventIdEsperado)) {
                        return registro.value();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Colaboradores do caminho de consumo sem infraestrutura: read model e marcador em memoria, e a
     * projecao real falhando para a viagem "venenosa" (o que o DLT precisa absorver).
     */
    @TestConfiguration
    static class ColaboradoresEmMemoria {

        @Bean
        @Primary
        PosicaoReadModelEmMemoria readModel() {
            return new PosicaoReadModelEmMemoria() {
                @Override
                public Mono<Void> salvarViagem(ViagemTrackingView view) {
                    if (VIAGEM_VENENOSA.equals(view.getViagemId())) {
                        return Mono.error(new IllegalStateException("falha injetada na projecao"));
                    }
                    return super.salvarViagem(view);
                }
            };
        }

        @Bean
        @Primary
        EventoProcessadoRepository marcador() {
            return new EventoProcessadoEmMemoria();
        }

        @Bean
        @Primary
        PosicaoPublisher publisher() {
            return (viagemId, payload) -> Mono.empty();
        }
    }
}
