package com.iprody.inventory.kafka.event;

import com.iprody.common.kafka.CancellationRequest;
import com.iprody.common.utils.UUIDUtils;
import com.iprody.inventory.service.EventProcessorService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancellationRequestListener {
    public static final String TOPIC = "cancellation.request";

    private final EventProcessorService eventProcessorService;
    private final Validator validator;

    @KafkaListener(
            topics = TOPIC,
            groupId = "inventory-cancellation-group",
            containerFactory = "cancellationListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, CancellationRequest> record, Acknowledgment ack) {
        log.info("Received cancellation request with key={}", record.key());
        log.info("Received cancellation request from topic: {}, partition: {}, offset: {}",
                record.topic(), record.partition(), record.offset());

        if (!validate(record)) {
            return;
        }

        try {
            eventProcessorService.processCancellationRequest(UUID.fromString(record.key()), record.value());
            ack.acknowledge();
            log.info("Request processed cancelled successfully for event={}", record.value());
        } catch (Exception e) {
            log.error("Failed to process cancellation request for record group id={}: {}", record.key(), e.getMessage(), e);
        }
    }

    private boolean validate(ConsumerRecord<String, CancellationRequest> record) {
        if (!UUIDUtils.isUUID(record.key())) {
            log.info("Received unsupported key={}, for record group id={}", record.key(), record.value().getId());
            return false;
        }

        return validator.validate(record.value()).stream()
                .findFirst()
                .map(v -> {
                    log.info("Validation failed for key {}: {} {}", record.key(), v.getPropertyPath(), v.getMessage());
                    return false;
                })
                .orElse(true);
    }
}
