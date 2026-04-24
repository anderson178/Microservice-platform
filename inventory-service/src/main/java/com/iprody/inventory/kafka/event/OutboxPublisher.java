package com.iprody.inventory.kafka.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iprody.inventory.model.OutboxEvent;
import com.iprody.inventory.model.OutboxEventStatus;
import com.iprody.inventory.model.OutboxEventType;
import com.iprody.inventory.repository.OutboxEventRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxEventRepo outboxRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.outbox.batch-size:50}")
    private int batchSize;


    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        log.info("Polling outbox for pending events (batchSize={})", batchSize);
        List<OutboxEvent> pending = outboxRepo.findPendingEvents(PageRequest.of(0, batchSize));

        if (pending.isEmpty()) {
            return;
        }

        log.info("Found {} pending outbox events to publish", pending.size());
        pending.forEach(this::publishEvent);
    }

    private void publishEvent(OutboxEvent event) {
        try {
            kafkaTemplate.send(
                            resolveTopic(event.getEventType()),
                            event.getAggregateId().toString(),
                            objectMapper.readValue(event.getEvent(), Object.class))
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            markAsPublished(event);
                            log.info("Published outbox event: id={}, eventType={}", event.getId(), event.getEventType());
                        } else {
                            markAsFailed(event);
                            log.error("Failed to publish outbox event: id={}, eventType={}",
                                    event.getId(), event.getEventType(), ex);
                        }
                    });
        } catch (JsonProcessingException e) {
            markAsFailed(event);
            log.error("JSON mapping failed for outbox event {}: {}", event.getId(), e.getMessage());
        }
    }

    private String resolveTopic(OutboxEventType eventType) {
        return switch (eventType) {
            case CANCELLATION_REQUESTED -> CancellationRequestListener.TOPIC;
        };
    }

    private void markAsPublished(OutboxEvent event) {
        event.setStatus(OutboxEventStatus.PUBLISHED);
        event.setProcessedAt(Instant.now());
        outboxRepo.save(event);
    }

    private void markAsFailed(OutboxEvent event) {
        event.setStatus(OutboxEventStatus.FAILED);
        event.setProcessedAt(Instant.now());
        outboxRepo.save(event);
    }
}
