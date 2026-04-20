package com.iprody.inquiry.kafka;

import com.iprody.inquiry.service.EventProcessorService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancellationResponseListener {
    private static final String TOPIC = "cancellation.response";

    private final EventProcessorService eventProcessorService;
    private final Validator validator;

    @KafkaListener(
            topics = TOPIC,
            groupId = "inquiry-cancellation-group",
            containerFactory = "cancellationListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, CancellationResponse> record, Acknowledgment ack) {
        log.info("Received cancellation response with key={}", record.key());
        log.debug("Received cancellation response from topic: {}, partition: {}, offset: {}",
                record.topic(), record.partition(), record.offset());

        CancellationResponse event = record.value();
        Set<ConstraintViolation<CancellationResponse>> violations = validator.validate(event);

        if (!violations.isEmpty()) {
            log.error("Validation failed for message key={}: {}", record.key(), violations);
            return;
        }

        try {
            eventProcessorService.processCancellationResponse(event);
            ack.acknowledge();
            log.info("Response processed cancelled successfully for event={}", event);
        } catch (Exception e) {
            log.error("Failed to process cancellation response for inquiryId={}: {}", record.key(), e.getMessage(), e);
        }
    }
}
