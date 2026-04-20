package com.iprody.inquiry.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("CancellationEventPublisher Unit Tests")
class CancellationEventPublisherTest {
    @Mock
    private KafkaTemplate<String, CancellationRequest> kafkaTemplate;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, CancellationRequest>> recordCaptor;

    private CancellationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new CancellationEventPublisher(kafkaTemplate);
    }

    @Test
    @DisplayName("should send CancellationRequest to correct topic with key")
    void publish_sendsToCorrectTopicWithKey() {
        UUID inquiryId = UUID.randomUUID();
        CancellationRequest event = new CancellationRequest();
        event.setId(inquiryId);
        event.setReason("Test reason");

        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, CancellationRequest>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        publisher.publish(event);

        verify(kafkaTemplate).send(recordCaptor.capture());
        ProducerRecord<String, CancellationRequest> captured = recordCaptor.getValue();

        assertThat(captured.topic()).isEqualTo("cancellation.request");
        assertThat(captured.key()).isEqualTo(inquiryId.toString());
        assertThat(captured.value()).isSameAs(event);
    }

    @Test
    @DisplayName("should log error when Kafka send fails")
    void publish_kafkaFailure_logsError() {
        CancellationRequest event = new CancellationRequest();
        event.setId(UUID.randomUUID());

        CompletableFuture<SendResult<String, CancellationRequest>> failedFuture =
                new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Connection refused"));

        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(failedFuture);

        publisher.publish(event);

        verify(kafkaTemplate).send(any(ProducerRecord.class));
    }
}
