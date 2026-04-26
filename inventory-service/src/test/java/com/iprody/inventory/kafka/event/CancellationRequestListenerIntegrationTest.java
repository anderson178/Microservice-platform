package com.iprody.inventory.kafka.event;

import com.iprody.common.kafka.CancellationRequest;
import com.iprody.common.kafka.CancellationStatus;
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
@DisplayName("CancellationRequestListener Integration Test")
class CancellationRequestListenerIntegrationTest {
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
        @DisplayName("should process RECEIVED request with available reservation and save event with SUCCESS status")
        void consume_receivedRequest_withAvailableReservation_savesSuccessEvent() {
            UUID inquiryId = UUID.randomUUID();
            UUID groupRefId = UUID.randomUUID();
            createGroup(groupRefId);

            CancellationRequest request = new CancellationRequest();
            request.setId(groupRefId);
            request.setStatus(CancellationStatus.RECEIVED);
            request.setReason("Customer requested cancellation");

            kafkaTemplate.send(OutboxEventType.CANCELLATION_REQUESTED.getListenTopic(), inquiryId.toString(), request);

            await()
                    .atMost(20, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryId, OutboxEventType.CANCELLATION_REQUESTED);

                        assertThat(events).isNotEmpty();
                        OutboxEvent saved = events.get(0);

                        assertThat(saved.getAggregateType()).isEqualTo(OutboxAggregateType.GROUP);
                        assertThat(saved.getAggregateId()).isEqualTo(inquiryId);
                        assertThat(saved.getEventType()).isEqualTo(OutboxEventType.CANCELLATION_REQUESTED);
                        assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);

                        assertThat(saved.getEvent()).contains("\"status\": \"SUCCESS\"");
                        assertThat(saved.getEvent()).contains("\"reason\": \"Customer requested cancellation\"");

                        Group updatedGroup = groupRepo.findByGroupRefId(groupRefId).orElseThrow();
                        assertThat(updatedGroup.getCurrentCount()).isEqualTo(4L);
                    });
        }

        @Test
        @DisplayName("should handle idempotency: duplicate request with same aggregateId is ignored")
        void consume_duplicateRequest_isIgnored_dueToIdempotency() {
            UUID inquiryId = UUID.randomUUID();
            UUID groupRefId = UUID.randomUUID();
            createGroup(groupRefId);

            CancellationRequest request = new CancellationRequest();
            request.setId(groupRefId);
            request.setStatus(CancellationStatus.RECEIVED);
            request.setReason("First request");

            kafkaTemplate.send(OutboxEventType.CANCELLATION_REQUESTED.getListenTopic(), inquiryId.toString(), request);
            // Duplicate
            kafkaTemplate.send(OutboxEventType.CANCELLATION_REQUESTED.getListenTopic(), inquiryId.toString(), request);

            await()
                    .atMost(20, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryId, OutboxEventType.CANCELLATION_REQUESTED);

                        assertThat(events).hasSize(1);

                        OutboxEvent saved = events.get(0);
                        assertThat(saved.getEvent()).contains("\"status\": \"SUCCESS\"");

                        Group updatedGroup = groupRepo.findByGroupRefId(groupRefId).orElseThrow();
                        assertThat(updatedGroup.getCurrentCount()).isEqualTo(4L);
                    });
        }
    }

    @Nested
    @DisplayName("Negative")
    class NegativeTests {

        @Test
        @DisplayName("should not process request with non-RECEIVED status (skipped by business logic)")
        void consume_nonReceivedStatus_isSkipped() {
            UUID inquiryId = UUID.randomUUID();
            UUID groupRefId = UUID.randomUUID();
            createGroup(groupRefId);

            CancellationRequest request = new CancellationRequest();
            request.setId(groupRefId);
            request.setStatus(CancellationStatus.SUCCESS);
            request.setReason("Already processed");

            kafkaTemplate.send(OutboxEventType.CANCELLATION_REQUESTED.getListenTopic(), inquiryId.toString(), request);

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryId, OutboxEventType.CANCELLATION_REQUESTED);
                        assertThat(events).isEmpty();

                        Group unchanged = groupRepo.findByGroupRefId(groupRefId).orElseThrow();
                        assertThat(unchanged.getCurrentCount()).isEqualTo(5L);
                    });
        }

        @Test
        @DisplayName("should not process request when group does not exist (validation fails)")
        void consume_nonExistentGroup_doesNotPersist() {
            UUID inquiryId = UUID.randomUUID();
            UUID nonExistentGroupRefId = UUID.randomUUID();

            CancellationRequest request = new CancellationRequest();
            request.setId(nonExistentGroupRefId);
            request.setStatus(CancellationStatus.RECEIVED);

            kafkaTemplate.send(OutboxEventType.CANCELLATION_REQUESTED.getListenTopic(), inquiryId.toString(), request);

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryId, OutboxEventType.CANCELLATION_REQUESTED);
                        assertThat(events).isEmpty();
                    });
        }

        @Test
        @DisplayName("should not persist when CancellationRequest id is null (violates @NotNull)")
        void consume_nullDtoId_doesNotPersist() {
            UUID inquiryId = UUID.randomUUID();
            UUID groupRefId = UUID.randomUUID();
            createGroup(groupRefId);

            CancellationRequest request = new CancellationRequest();
            request.setId(null);
            request.setStatus(CancellationStatus.RECEIVED);

            kafkaTemplate.send(OutboxEventType.CANCELLATION_REQUESTED.getListenTopic(), groupRefId.toString(), request);

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryId, OutboxEventType.CANCELLATION_REQUESTED);
                        assertThat(events).isEmpty();
                    });
        }

        @Test
        @DisplayName("should not persist when DTO status is null (violates @NotNull)")
        void consume_nullDtoStatus_doesNotPersist() {
            UUID inquiryId = UUID.randomUUID();
            UUID groupRefId = UUID.randomUUID();
            createGroup(groupRefId);

            CancellationRequest request = new CancellationRequest();
            request.setId(groupRefId);
            request.setStatus(null);

            kafkaTemplate.send(OutboxEventType.CANCELLATION_REQUESTED.getListenTopic(), groupRefId.toString(), request);

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryId, OutboxEventType.CANCELLATION_REQUESTED);
                        assertThat(events).isEmpty();
                    });
        }
    }

    private void createGroup(UUID groupRefId) {
        Group group = new Group();
        group.setGroupRefId(groupRefId);
        group.setCurrentCount(5L);
        group.setLimit(10L);

        groupRepo.save(group);
        groupRepo.flush();
    }
}
