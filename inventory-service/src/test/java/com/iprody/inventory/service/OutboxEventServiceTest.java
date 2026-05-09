package com.iprody.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iprody.common.kafka.CancellationRequest;
import com.iprody.common.kafka.CancellationStatus;
import com.iprody.inventory.configuration.ConfigurationTest;
import com.iprody.inventory.model.OutboxAggregateType;
import com.iprody.inventory.model.OutboxEvent;
import com.iprody.inventory.model.OutboxEventStatus;
import com.iprody.inventory.model.OutboxEventType;
import com.iprody.inventory.repository.OutboxEventRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Import(ConfigurationTest.class)
@DisplayName("OutboxEventService Unit Tests")
class OutboxEventServiceTest {
    @Mock
    private OutboxEventRepo outboxRepo;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxEventService outboxEventService;

    @Captor
    private ArgumentCaptor<OutboxEvent> eventCaptor;


    @Nested
    @DisplayName("saveEvent() — Positive")
    class SaveEventPositiveTests {

        @Test
        @DisplayName("should serialize CancellationRequest and save to outbox with PENDING status")
        void saveEvent_validCancellationRequest_savesSuccessfully() throws JsonProcessingException {
            UUID inquiryId = UUID.randomUUID();
            OutboxAggregateType aggregateType = OutboxAggregateType.GROUP;
            OutboxEventType eventType = OutboxEventType.CANCELLATION_REQUEST;

            CancellationRequest payload = new CancellationRequest();
            payload.setId(inquiryId);
            payload.setStatus(CancellationStatus.RECEIVED);
            payload.setReason("Customer requested cancellation");

            String expectedJson = String.format(
                    "{\"id\":\"%s\",\"status\":\"%s\",\"reason\":\"Customer requested cancellation\"}",
                    inquiryId, CancellationStatus.RECEIVED.name()
            );

            when(objectMapper.writeValueAsString(payload)).thenReturn(expectedJson);

            outboxEventService.saveEvent(aggregateType, inquiryId, eventType, payload);

            verify(outboxRepo).save(eventCaptor.capture());
            OutboxEvent saved = eventCaptor.getValue();

            assertThat(saved.getAggregateType()).isEqualTo(aggregateType);
            assertThat(saved.getAggregateId()).isEqualTo(inquiryId);
            assertThat(saved.getEventType()).isEqualTo(eventType);
            assertThat(saved.getEvent()).isEqualTo(expectedJson);
            assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(saved.getId()).isNull();
        }

        @Test
        @DisplayName("should handle CancellationRequest with null reason (optional field)")
        void saveEvent_nullReason_serializesCorrectly() throws JsonProcessingException {
            UUID inquiryId = UUID.randomUUID();
            CancellationRequest payload = new CancellationRequest();
            payload.setId(inquiryId);
            payload.setStatus(CancellationStatus.RECEIVED);

            String expectedJson = "{\"id\":\"" + inquiryId + "\",\"status\":\"RECEIVED\"}";  // или с "reason":null
            when(objectMapper.writeValueAsString(payload)).thenReturn(expectedJson);

            outboxEventService.saveEvent(
                    OutboxAggregateType.GROUP,
                    inquiryId,
                    OutboxEventType.CANCELLATION_REQUEST,
                    payload
            );

            verify(outboxRepo).save(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getEvent()).isEqualTo(expectedJson);
            verify(objectMapper).writeValueAsString(payload);
        }
    }

    @Nested
    @DisplayName("saveEvent() — Negative")
    class SaveEventNegativeTests {

        @Test
        @DisplayName("should throw RuntimeException when CancellationRequest serialization fails")
        void saveEvent_serializationFailure_throwsException() throws JsonProcessingException {
            CancellationRequest payload = new CancellationRequest();
            payload.setId(UUID.randomUUID());
            payload.setStatus(CancellationStatus.RECEIVED);

            when(objectMapper.writeValueAsString(payload))
                    .thenThrow(new JsonProcessingException("Cannot serialize CancellationRequest") {
                    });

            assertThatThrownBy(() ->
                    outboxEventService.saveEvent(
                            OutboxAggregateType.GROUP,
                            payload.getId(),
                            OutboxEventType.CANCELLATION_REQUEST,
                            payload
                    ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to serialize outbox event")
                    .hasCauseInstanceOf(JsonProcessingException.class);

            verify(outboxRepo, never()).save(any());
            verify(objectMapper).writeValueAsString(payload);
        }

        @Test
        @DisplayName("should not save to repo when payload has invalid UUID format (handled by Jackson)")
        void saveEvent_invalidUuidInPayload_serializationFails() throws JsonProcessingException {
            CancellationRequest payload = new CancellationRequest();

            when(objectMapper.writeValueAsString(payload))
                    .thenThrow(new JsonProcessingException("Invalid UUID format") {
                    });

            assertThatThrownBy(() ->
                    outboxEventService.saveEvent(
                            OutboxAggregateType.GROUP,
                            UUID.randomUUID(),
                            OutboxEventType.CANCELLATION_REQUEST,
                            payload
                    ))
                    .isInstanceOf(RuntimeException.class);

            verify(outboxRepo, never()).save(any());
        }

        @Test
        @DisplayName("should handle null CancellationRequest (delegates to ObjectMapper behavior)")
        void saveEvent_nullPayload_delegatesToObjectMapper() throws JsonProcessingException {
            when(objectMapper.writeValueAsString(null)).thenReturn("null");

            outboxEventService.saveEvent(
                    OutboxAggregateType.GROUP,
                    UUID.randomUUID(),
                    OutboxEventType.CANCELLATION_REQUEST,
                    null
            );

            verify(outboxRepo).save(argThat(event ->
                    event.getEvent().equals("null") &&
                            event.getStatus() == OutboxEventStatus.PENDING
            ));
        }
    }
}
