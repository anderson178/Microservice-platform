package com.iprody.inventory.kafka.event;

import com.iprody.common.kafka.InventoryRequest;
import com.iprody.inventory.configuration.KafkaTestConfig;
import com.iprody.inventory.configuration.PostgresTestConfig;
import com.iprody.inventory.model.*;
import com.iprody.inventory.repository.GroupRepo;
import com.iprody.inventory.repository.OutboxEventRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Import({PostgresTestConfig.class, KafkaTestConfig.class})
@DisplayName("InventoryAvailabilityRequestListener Integration Test")
public class InventoryRequestListenerIT {
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private OutboxEventRepo outboxEventRepo;

    @Autowired
    private GroupRepo groupRepo;

    @BeforeEach
    void setUp() {
        outboxEventRepo.deleteAll();
        groupRepo.deleteAll();
    }

    @Nested
    @DisplayName("Positive")
    class PositiveTests {

        @Test
        @DisplayName("should process request with available seats and save event with RESERVED status")
        void consume_requestWithAvailableSeats_savesAvailableEvent() {
            UUID inquiryRefId = UUID.randomUUID();
            UUID groupRefId = UUID.randomUUID();
            createGroup(groupRefId, 5L);

            kafkaTemplate.send(
                    OutboxEventType.INVENTORY_REQUEST.getTopic(),
                    inquiryRefId.toString(),
                    new InventoryRequest(inquiryRefId, groupRefId, 3L)
            );

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryRefId, OutboxEventType.INVENTORY_RESPONSE);

                        assertThat(events).isNotEmpty();
                        OutboxEvent saved = events.get(0);

                        assertThat(saved.getAggregateType()).isEqualTo(OutboxAggregateType.GROUP);
                        assertThat(saved.getAggregateId()).isEqualTo(inquiryRefId);
                        assertThat(saved.getEventType()).isEqualTo(OutboxEventType.INVENTORY_RESPONSE);
                        assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);

                        assertThat(saved.getEvent()).contains("\"status\": \"RESERVED\"");
                        assertThat(saved.getEvent()).contains("\"groupRefId\": \"" + groupRefId + "\"");
                    });
        }

        @Test
        @DisplayName("should process request with insufficient seats and save event with ROLLBACK status")
        void consume_requestWithInsufficientSeats_savesNotAvailableEvent() {
            UUID inquiryRefId = UUID.randomUUID();
            UUID groupRefId = UUID.randomUUID();
            createGroup(groupRefId, 8L);

            kafkaTemplate.send(
                    OutboxEventType.INVENTORY_REQUEST.getTopic(),
                    inquiryRefId.toString(),
                    new InventoryRequest(inquiryRefId, groupRefId, 5L)
            );

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryRefId, OutboxEventType.INVENTORY_RESPONSE);

                        assertThat(events).isNotEmpty();
                        OutboxEvent saved = events.get(0);

                        assertThat(saved.getAggregateType()).isEqualTo(OutboxAggregateType.GROUP);
                        assertThat(saved.getAggregateId()).isEqualTo(inquiryRefId);
                        assertThat(saved.getEventType()).isEqualTo(OutboxEventType.INVENTORY_RESPONSE);
                        assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);

                        assertThat(saved.getEvent()).contains("\"status\": \"ROLLBACK\"");
                        assertThat(saved.getEvent()).contains("\"groupRefId\": \"" + groupRefId + "\"");
                    });
        }

        @Test
        @DisplayName("should process request with exact available seats count")
        void consume_requestWithExactAvailableSeats_savesAvailableEvent() {
            UUID inquiryRefId = UUID.randomUUID();
            UUID groupRefId = UUID.randomUUID();
            createGroup(groupRefId, 5L);

            kafkaTemplate.send(
                    OutboxEventType.INVENTORY_REQUEST.getTopic(),
                    inquiryRefId.toString(),
                    new InventoryRequest(inquiryRefId, groupRefId, 5L)
            );

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryRefId, OutboxEventType.INVENTORY_RESPONSE);

                        assertThat(events).isNotEmpty();
                        OutboxEvent saved = events.get(0);

                        assertThat(saved.getEvent()).contains("\"status\": \"RESERVED\"");
                    });
        }
    }

    @Nested
    @DisplayName("Negative")
    class NegativeTests {

        @Test
        @DisplayName("should not process request when group does not exist (validation fails)")
        void consume_nonExistentGroup_doesNotPersist() {
            UUID inquiryRefId = UUID.randomUUID();

            kafkaTemplate.send(
                    OutboxEventType.INVENTORY_REQUEST.getTopic(),
                    inquiryRefId.toString(),
                    new InventoryRequest(inquiryRefId, UUID.randomUUID(), 1L)
            );

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryRefId, OutboxEventType.INVENTORY_RESPONSE);
                        assertThat(events).isEmpty();
                    });
        }

        @Test
        @DisplayName("should not persist when inquiryRefId is null (violates @NotNull)")
        void consume_nullInquiryRefId_doesNotPersist() {
            UUID groupRefId = UUID.randomUUID();

            kafkaTemplate.send(
                    OutboxEventType.INVENTORY_REQUEST.getTopic(),
                    groupRefId.toString(),
                    new InventoryRequest(null, UUID.randomUUID(), 1L)
            );

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<OutboxEvent> events = outboxEventRepo.findAll();
                        assertThat(events).isEmpty();
                    });
        }

        @Test
        @DisplayName("should not persist when groupRefId is null (violates @NotNull)")
        void consume_nullGroupRefId_doesNotPersist() {
            UUID inquiryRefId = UUID.randomUUID();

            kafkaTemplate.send(
                    OutboxEventType.INVENTORY_REQUEST.getTopic(),
                    inquiryRefId.toString(),
                    new InventoryRequest(inquiryRefId, null, 1L)
            );

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryRefId, OutboxEventType.INVENTORY_RESPONSE);
                        assertThat(events).isEmpty();
                    });
        }

        @Test
        @DisplayName("should not persist when numberOfSeats is null (violates @NotNull)")
        void consume_nullNumberOfSeats_doesNotPersist() {
            UUID inquiryRefId = UUID.randomUUID();

            kafkaTemplate.send(
                    OutboxEventType.INVENTORY_REQUEST.getTopic(),
                    inquiryRefId.toString(),
                    new InventoryRequest(inquiryRefId, UUID.randomUUID(), null)
            );

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryRefId, OutboxEventType.INVENTORY_RESPONSE);
                        assertThat(events).isEmpty();
                    });
        }
    }

    private void createGroup(UUID groupRefId, Long currentCount) {
        Group group = new Group();
        group.setGroupRefId(groupRefId);
        group.setLimit(10L);
        group.setCurrentCount(currentCount);

        groupRepo.save(group);
        groupRepo.flush();
    }
}
