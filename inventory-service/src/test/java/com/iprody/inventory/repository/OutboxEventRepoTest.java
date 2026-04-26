package com.iprody.inventory.repository;

import com.iprody.inventory.configuration.PostgresTestConfig;
import com.iprody.inventory.model.OutboxAggregateType;
import com.iprody.inventory.model.OutboxEvent;
import com.iprody.inventory.model.OutboxEventStatus;
import com.iprody.inventory.model.OutboxEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(PostgresTestConfig.class)
@DisplayName("OutboxEventRepo Integration Tests")
class OutboxEventRepoTest {
    @Autowired
    private OutboxEventRepo outboxEventRepo;

    @BeforeEach
    void setUp() {
        outboxEventRepo.deleteAll();
    }


    @Nested
    @DisplayName("findPendingEvents()")
    class FindPendingEventsTests {

        @Nested
        @DisplayName("Positive")
        class PositiveTests {

            @Test
            @DisplayName("should return pending events ordered by createdAt ascending")
            void findPendingEvents_returnsOrderedByCreatedAt() {
                OutboxEvent oldEvent = createPendingEvent(
                        OutboxAggregateType.GROUP, UUID.randomUUID(),
                        OutboxEventType.CANCELLATION_REQUESTED,
                        Instant.now().minusSeconds(100)
                );
                OutboxEvent middleEvent = createPendingEvent(
                        OutboxAggregateType.GROUP, UUID.randomUUID(),
                        OutboxEventType.CANCELLATION_REQUESTED,
                        Instant.now().minusSeconds(50)
                );
                OutboxEvent newEvent = createPendingEvent(
                        OutboxAggregateType.GROUP, UUID.randomUUID(),
                        OutboxEventType.CANCELLATION_REQUESTED,
                        Instant.now()
                );

                List<OutboxEvent> result = outboxEventRepo.findPendingEvents(Pageable.unpaged());

                assertThat(result).hasSize(3);
                assertThat(result)
                        .extracting(OutboxEvent::getId)
                        .containsExactly(oldEvent.getId(), middleEvent.getId(), newEvent.getId());
                assertThat(result)
                        .extracting(OutboxEvent::getCreatedAt)
                        .isSorted();
            }

            @Test
            @DisplayName("should return only events with PENDING status")
            void findPendingEvents_filtersByPendingStatus() {
                OutboxEvent pending1 = createPendingEvent(
                        OutboxAggregateType.GROUP, UUID.randomUUID(),
                        OutboxEventType.CANCELLATION_REQUESTED, Instant.now()
                );
                OutboxEvent pending2 = createPendingEvent(
                        OutboxAggregateType.GROUP, UUID.randomUUID(),
                        OutboxEventType.CANCELLATION_REQUESTED, Instant.now().minusSeconds(5)
                );

                List<OutboxEvent> result = outboxEventRepo.findPendingEvents(Pageable.unpaged());

                assertThat(result).hasSize(2);
                assertThat(result)
                        .extracting(OutboxEvent::getId)
                        .containsExactly(pending1.getId(), pending2.getId());
                assertThat(result)
                        .allMatch(e -> e.getStatus() == OutboxEventStatus.PENDING);
            }

            @Test
            @DisplayName("should apply pagination correctly")
            void findPendingEvents_withPagination_returnsCorrectPage() {
                for (int i = 0; i < 10; i++) {
                    createPendingEvent(
                            OutboxAggregateType.GROUP, UUID.randomUUID(),
                            OutboxEventType.CANCELLATION_REQUESTED,
                            Instant.now().minusSeconds(100 - i * 10)
                    );
                }

                Pageable pageable = PageRequest.of(1, 3);
                List<OutboxEvent> result = outboxEventRepo.findPendingEvents(pageable);

                assertThat(result).hasSize(3);
                assertThat(result)
                        .extracting(OutboxEvent::getCreatedAt)
                        .isSorted();
            }

            @Test
            @DisplayName("should return empty list when no pending events exist")
            void findPendingEvents_noPending_returnsEmpty() {
                createEventWithStatus(OutboxAggregateType.GROUP, UUID.randomUUID(),
                        OutboxEventType.CANCELLATION_REQUESTED,
                        OutboxEventStatus.PUBLISHED, Instant.now());
                createEventWithStatus(OutboxAggregateType.GROUP, UUID.randomUUID(),
                        OutboxEventType.CANCELLATION_REQUESTED,
                        OutboxEventStatus.FAILED, Instant.now());

                List<OutboxEvent> result = outboxEventRepo.findPendingEvents(Pageable.unpaged());

                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("Negative")
        class NegativeTests {

            @Test
            @DisplayName("should return empty list when table is empty")
            void findPendingEvents_emptyTable_returnsEmpty() {
                List<OutboxEvent> result = outboxEventRepo.findPendingEvents(Pageable.unpaged());

                assertThat(result).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("existsByAggregateIdAndEventType()")
    class ExistsByAggregateIdAndEventTypeTests {

        @Nested
        @DisplayName("Positive")
        class PositiveTests {

            @Test
            @DisplayName("should return true when event exists with matching aggregateId and eventType")
            void existsByAggregateIdAndEventType_found_returnsTrue() {
                UUID aggregateId = UUID.randomUUID();
                OutboxEventType eventType = OutboxEventType.CANCELLATION_REQUESTED;

                createPendingEvent(OutboxAggregateType.GROUP, aggregateId, eventType, Instant.now());

                boolean exists = outboxEventRepo.existsByAggregateIdAndEventType(aggregateId, eventType);

                assertThat(exists).isTrue();
            }

            @Test
            @DisplayName("should return true regardless of event status")
            void existsByAggregateIdAndEventType_ignoresStatus_returnsTrue() {
                UUID aggregateId = UUID.randomUUID();
                OutboxEventType eventType = OutboxEventType.CANCELLATION_REQUESTED;

                createEventWithStatus(OutboxAggregateType.GROUP, aggregateId, eventType,
                        OutboxEventStatus.PUBLISHED, Instant.now().minusSeconds(100));
                createEventWithStatus(OutboxAggregateType.GROUP, aggregateId, eventType,
                        OutboxEventStatus.FAILED, Instant.now().minusSeconds(50));
                createPendingEvent(OutboxAggregateType.GROUP, aggregateId, eventType, Instant.now());

                boolean exists = outboxEventRepo.existsByAggregateIdAndEventType(aggregateId, eventType);

                assertThat(exists).isTrue();
            }

            @Test
            @DisplayName("should return true when multiple events match (at least one exists)")
            void existsByAggregateIdAndEventType_multipleMatches_returnsTrue() {
                UUID aggregateId = UUID.randomUUID();
                OutboxEventType eventType = OutboxEventType.CANCELLATION_REQUESTED;

                createPendingEvent(OutboxAggregateType.GROUP, aggregateId, eventType, Instant.now().minusSeconds(10));

                boolean exists = outboxEventRepo.existsByAggregateIdAndEventType(aggregateId, eventType);

                assertThat(exists).isTrue();
            }
        }

        @Nested
        @DisplayName("Negative")
        class NegativeTests {

            @Test
            @DisplayName("should return false when no event matches aggregateId and eventType")
            void existsByAggregateIdAndEventType_notFound_returnsFalse() {
                UUID nonExistentAggregateId = UUID.randomUUID();
                OutboxEventType eventType = OutboxEventType.CANCELLATION_REQUESTED;

                createPendingEvent(OutboxAggregateType.GROUP, UUID.randomUUID(), eventType, Instant.now());

                boolean exists = outboxEventRepo.existsByAggregateIdAndEventType(nonExistentAggregateId, eventType);

                assertThat(exists).isFalse();
            }

            @Test
            @DisplayName("should return false when eventType matches but aggregateId differs")
            void existsByAggregateIdAndEventType_wrongAggregateId_returnsFalse() {
                OutboxEventType eventType = OutboxEventType.CANCELLATION_REQUESTED;

                createPendingEvent(OutboxAggregateType.GROUP, UUID.randomUUID(), eventType, Instant.now());

                boolean exists = outboxEventRepo.existsByAggregateIdAndEventType(
                        UUID.randomUUID(), eventType);

                assertThat(exists).isFalse();
            }

            @Test
            @DisplayName("should return false when table is empty")
            void existsByAggregateIdAndEventType_emptyTable_returnsFalse() {
                boolean exists = outboxEventRepo.existsByAggregateIdAndEventType(
                        UUID.randomUUID(), OutboxEventType.CANCELLATION_REQUESTED);

                assertThat(exists).isFalse();
            }

            @Test
            @DisplayName("should return false when aggregateId is null")
            void existsByAggregateIdAndEventType_nullAggregateId_returnsFalse() {
                boolean exists = outboxEventRepo.existsByAggregateIdAndEventType(
                        null, OutboxEventType.CANCELLATION_REQUESTED);

                assertThat(exists).isFalse();
            }

            @Test
            @DisplayName("should return false when eventType is null")
            void existsByAggregateIdAndEventType_nullEventType_returnsFalse() {
                boolean exists = outboxEventRepo.existsByAggregateIdAndEventType(
                        UUID.randomUUID(), null);

                assertThat(exists).isFalse();
            }
        }
    }

    private OutboxEvent createPendingEvent(OutboxAggregateType aggregateType, UUID aggregateId,
                                           OutboxEventType eventType, Instant createdAt) {
        return createEventWithStatus(aggregateType, aggregateId, eventType,
                OutboxEventStatus.PENDING, createdAt);
    }

    private OutboxEvent createEventWithStatus(OutboxAggregateType aggregateType, UUID aggregateId,
                                              OutboxEventType eventType, OutboxEventStatus status,
                                              Instant createdAt) {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .event("{\"test\":\"data\"}")  // Простой валидный JSON
                .status(status)
                .createdAt(createdAt)
                .build();
        return outboxEventRepo.save(event);
    }
}