package com.iprody.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iprody.common.kafka.PaymentRequest;
import com.iprody.common.struct.PaymentStatus;
import com.iprody.payment.configuration.ConfigurationTest;
import com.iprody.payment.model.outbox.OutboxAggregateType;
import com.iprody.payment.model.outbox.OutboxEvent;
import com.iprody.payment.model.outbox.OutboxEventStatus;
import com.iprody.payment.model.outbox.OutboxEventType;
import com.iprody.payment.repository.OutboxEventRepo;
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
        @DisplayName("should serialize PaymentRequest and save to outbox with PENDING status")
        void saveEvent_validCancellationRequest_savesSuccessfully() throws JsonProcessingException {
            UUID inquiryId = UUID.randomUUID();
            OutboxAggregateType aggregateType = OutboxAggregateType.INQUIRY;
            OutboxEventType eventType = OutboxEventType.PAYMENT_REQUESTED;

            PaymentRequest payload = new PaymentRequest();
            payload.setInquiryRefId(inquiryId);
            payload.setStatus(PaymentStatus.RECEIVED);

            outboxEventService.saveEvent(aggregateType, inquiryId, eventType, payload);

            verify(outboxRepo).save(eventCaptor.capture());
            OutboxEvent saved = eventCaptor.getValue();

            assertThat(saved.getAggregateType()).isEqualTo(aggregateType);
            assertThat(saved.getAggregateId()).isEqualTo(inquiryId);
            assertThat(saved.getEventType()).isEqualTo(eventType);
            assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(saved.getId()).isNull();
        }

        @Test
        @DisplayName("should handle PaymentRequest with null reason (optional field)")
        void saveEvent_nullReason_serializesCorrectly() throws JsonProcessingException {
            UUID inquiryId = UUID.randomUUID();
            PaymentRequest payload = new PaymentRequest();
            payload.setInquiryRefId(inquiryId);
            payload.setStatus(PaymentStatus.RECEIVED);

            String expectedJson = "{\"id\":\"" + inquiryId + "\",\"status\":\"RECEIVED\"}";
            when(objectMapper.writeValueAsString(payload)).thenReturn(expectedJson);

            outboxEventService.saveEvent(
                    OutboxAggregateType.INQUIRY,
                    inquiryId,
                    OutboxEventType.PAYMENT_REQUESTED,
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
        @DisplayName("should throw RuntimeException when PaymentRequest serialization fails")
        void saveEvent_serializationFailure_throwsException() throws JsonProcessingException {
            PaymentRequest payload = new PaymentRequest();
            payload.setInquiryRefId(UUID.randomUUID());
            payload.setStatus(PaymentStatus.RECEIVED);

            when(objectMapper.writeValueAsString(payload))
                    .thenThrow(new JsonProcessingException("Cannot serialize CancellationRequest") {
                    });

            assertThatThrownBy(() ->
                    outboxEventService.saveEvent(
                            OutboxAggregateType.INQUIRY,
                            payload.getInquiryRefId(),
                            OutboxEventType.PAYMENT_REQUESTED,
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
            PaymentRequest payload = new PaymentRequest();

            when(objectMapper.writeValueAsString(payload))
                    .thenThrow(new JsonProcessingException("Invalid UUID format") {
                    });

            assertThatThrownBy(() ->
                    outboxEventService.saveEvent(
                            OutboxAggregateType.INQUIRY,
                            UUID.randomUUID(),
                            OutboxEventType.PAYMENT_REQUESTED,
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
                    OutboxAggregateType.INQUIRY,
                    UUID.randomUUID(),
                    OutboxEventType.PAYMENT_REQUESTED,
                    null
            );

            verify(outboxRepo).save(argThat(event ->
                    event.getEvent().equals("null") &&
                            event.getStatus() == OutboxEventStatus.PENDING
            ));
        }
    }
}
