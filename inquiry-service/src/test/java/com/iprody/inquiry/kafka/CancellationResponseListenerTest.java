package com.iprody.inquiry.kafka;

import com.iprody.inquiry.service.EventProcessorService;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancellationResponseListener Unit Tests")
class CancellationResponseListenerTest {
    @Mock
    private EventProcessorService eventProcessorService;

    @Mock
    private Acknowledgment acknowledgment;

    @Mock
    private Validator validator;

    private CancellationResponseListener listener;

    @BeforeEach
    void setUp() {
        listener = new CancellationResponseListener(eventProcessorService, validator);
    }

    @Test
    @DisplayName("should process SUCCESS response and acknowledge")
    void consume_successResponse_processesAndAcks() {
        UUID inquiryId = UUID.randomUUID();
        CancellationResponse response = new CancellationResponse();
        response.setId(inquiryId);
        response.setStatus(CancellationStatus.SUCCESS);
        response.setReason("Approved");

        ConsumerRecord<String, CancellationResponse> record = createRecord(inquiryId.toString(), response);

        listener.consume(record, acknowledgment);

        verify(eventProcessorService).processCancellationResponse(response);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("should process REJECTED response and acknowledge")
    void consume_rejectedResponse_processesAndAcks() {
        UUID inquiryId = UUID.randomUUID();
        CancellationResponse response = new CancellationResponse();
        response.setId(inquiryId);
        response.setStatus(CancellationStatus.REJECTED);
        response.setReason("Insufficient funds");

        ConsumerRecord<String, CancellationResponse> record = createRecord(inquiryId.toString(), response);

        listener.consume(record, acknowledgment);

        verify(eventProcessorService).processCancellationResponse(response);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("should log error and NOT acknowledge when processing fails")
    void consume_processingFailure_logsErrorAndNoAck() {
        CancellationResponse response = new CancellationResponse();
        response.setId(UUID.randomUUID());

        ConsumerRecord<String, CancellationResponse> record = createRecord("key", response);
        doThrow(new RuntimeException("DB error"))
                .when(eventProcessorService).processCancellationResponse(response);

        listener.consume(record, acknowledgment);

        verify(eventProcessorService).processCancellationResponse(response);
        verifyNoInteractions(acknowledgment);
    }

    private ConsumerRecord<String, CancellationResponse> createRecord(String key, CancellationResponse value) {
        return new ConsumerRecord<>(
                "cancellation.response",
                0,
                0L,
                System.currentTimeMillis(),
                TimestampType.CREATE_TIME,
                0,
                0,
                key,
                value,
                new RecordHeaders(),
                Optional.empty()
        );
    }
}
