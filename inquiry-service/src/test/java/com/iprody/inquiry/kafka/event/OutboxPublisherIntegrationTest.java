package com.iprody.inquiry.kafka.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iprody.common.kafka.CancellationRequest;
import com.iprody.common.kafka.CancellationStatus;
import com.iprody.inquiry.configuration.KafkaTestConfig;
import com.iprody.inquiry.configuration.PostgresTestConfig;
import com.iprody.inquiry.model.OutboxAggregateType;
import com.iprody.inquiry.model.OutboxEvent;
import com.iprody.inquiry.model.OutboxEventStatus;
import com.iprody.inquiry.model.OutboxEventType;
import com.iprody.inquiry.repository.OutboxEventRepo;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Import({PostgresTestConfig.class, KafkaTestConfig.class})
@DisplayName("OutboxPublisher Integration Test")
class OutboxPublisherIntegrationTest {
    @Autowired
    private OutboxEventRepo outboxEventRepo;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OutboxPublisher outboxPublisher;

    private KafkaConsumer<String, Object> testConsumer;

    @BeforeEach
    void setUp() {
        outboxEventRepo.deleteAll();
        testConsumer = createTestConsumer();
    }

    @AfterEach
    void tearDown() {
        if (testConsumer != null) testConsumer.close();
    }

    @Nested
    @DisplayName("Positive")
    class PositiveTests {

        @Test
        @DisplayName("should publish PENDING events to correct Kafka topic and mark as PUBLISHED")
        void publishPendingEvents_success_updatesStatusAndSendsToKafka() throws JsonProcessingException, InterruptedException {
            UUID aggregateId = UUID.randomUUID();
            CancellationRequest payload = new CancellationRequest();
            payload.setId(aggregateId);
            payload.setStatus(CancellationStatus.RECEIVED);
            payload.setReason("Test cancellation");

            OutboxEvent event = createOutboxEvent(aggregateId, payload);
            outboxEventRepo.save(event);

            outboxPublisher.publishPendingEvents();

            kafkaTemplate.flush();
            Thread.sleep(300);

            String expectedTopic = OutboxEventType.CANCELLATION_RESPONSE.getTopic();
            subscribeToTopic(expectedTopic);

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(200, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<ConsumerRecord<String, Object>> records = pollRecords(Duration.ofMillis(200));
                        assertThat(records)
                                .anyMatch(r -> r.key().equals(aggregateId.toString()) &&
                                        r.value() instanceof CancellationRequest &&
                                        ((CancellationRequest) r.value()).getStatus() == CancellationStatus.RECEIVED);
                    });

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        OutboxEvent updated = outboxEventRepo.findById(event.getId()).orElseThrow();
                        assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
                        assertThat(updated.getProcessedAt()).isNotNull();
                    });
        }

        @Test
        @DisplayName("should skip non-PENDING events (PUBLISHED/FAILED)")
        void publishPendingEvents_ignoresNonPendingStatus() throws InterruptedException {
            UUID publishedId = UUID.randomUUID();
            UUID failedId = UUID.randomUUID();
            UUID pendingId = UUID.randomUUID();

            String expectedTopic = OutboxEventType.CANCELLATION_RESPONSE.getTopic();

            outboxEventRepo.save(createOutboxEventWithStatus(publishedId, OutboxEventStatus.PUBLISHED));
            outboxEventRepo.save(createOutboxEventWithStatus(failedId, OutboxEventStatus.FAILED));

            OutboxEvent pending = outboxEventRepo.save(
                    createOutboxEventWithStatus(pendingId, OutboxEventStatus.PENDING)
            );

            outboxPublisher.publishPendingEvents();

            kafkaTemplate.flush();
            Thread.sleep(300);

            var partition = new TopicPartition(expectedTopic, 0);
            testConsumer.assign(Collections.singletonList(partition));
            testConsumer.seekToBeginning(Collections.singletonList(partition));

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(200, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<ConsumerRecord<String, Object>> records = pollRecords(Duration.ofMillis(200));

                        assertThat(records).hasSize(1);
                        assertThat(records.iterator().next().key())
                                .isEqualTo(pending.getAggregateId().toString());
                    });
        }
    }

    @Nested
    @DisplayName("Negative")
    class NegativeTests {

        @Test
        @DisplayName("should do nothing when no PENDING events exist")
        void publishPendingEvents_emptyResult_noOp() {
            outboxEventRepo.save(createOutboxEventWithStatus(UUID.randomUUID(), OutboxEventStatus.PUBLISHED));
            outboxEventRepo.save(createOutboxEventWithStatus(UUID.randomUUID(), OutboxEventStatus.FAILED));

            outboxPublisher.publishPendingEvents();

            await()
                    .atMost(2, TimeUnit.SECONDS)
                    .pollInterval(200, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<ConsumerRecord<String, Object>> records = pollRecords(Duration.ofMillis(100));
                        assertThat(records).isEmpty();
                    });

            assertThat(outboxEventRepo.findAll())
                    .allMatch(e -> e.getStatus() != OutboxEventStatus.PENDING || e.getProcessedAt() == null);
        }
    }

    private OutboxEvent createOutboxEvent(UUID aggregateId, Object payload) throws JsonProcessingException {
        return OutboxEvent.builder()
                .aggregateType(OutboxAggregateType.INQUIRY)
                .aggregateId(aggregateId)
                .eventType(OutboxEventType.CANCELLATION_RESPONSE)
                .event(objectMapper.writeValueAsString(payload))
                .status(OutboxEventStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    private OutboxEvent createOutboxEventWithStatus(UUID aggregateId, OutboxEventStatus status) {
        return OutboxEvent.builder()
                .aggregateType(OutboxAggregateType.INQUIRY)
                .aggregateId(aggregateId)
                .eventType(OutboxEventType.CANCELLATION_RESPONSE)
                .event("{\"test\":\"data\"}")
                .status(status)
                .createdAt(Instant.now().minusSeconds(10))
                .build();
    }

    private KafkaConsumer<String, Object> createTestConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaTemplate.getProducerFactory().getConfigurationProperties().get("bootstrap.servers"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID() + "-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class);

        props.put("spring.json.trusted.packages", "com.iprody.common.kafka");
        props.put("spring.json.value.default.type", CancellationRequest.class.getName());
        props.put("spring.json.use.type.headers", false);

        KafkaConsumer<String, Object> consumer = new KafkaConsumer<>(props);

        TopicPartition partition = new TopicPartition("cancellation.response", 0);
        consumer.assign(Collections.singletonList(partition));

        return consumer;
    }

    private List<ConsumerRecord<String, Object>> pollRecords(Duration timeout) {
        List<ConsumerRecord<String, Object>> allRecords = new ArrayList<>();
        Instant endTime = Instant.now().plus(timeout);

        while (Instant.now().isBefore(endTime)) {
            ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofMillis(100));

            for (ConsumerRecord<String, Object> record : records) {
                allRecords.add(record);
            }
        }
        return allRecords;
    }

    private void subscribeToTopic(String topic) {
        var partition = new TopicPartition(topic, 0);
        testConsumer.assign(Collections.singletonList(partition));
        testConsumer.seekToBeginning(Collections.singletonList(partition));
    }
}
