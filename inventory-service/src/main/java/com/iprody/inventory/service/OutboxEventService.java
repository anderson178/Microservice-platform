package com.iprody.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iprody.inventory.model.OutboxEventType;
import com.iprody.inventory.model.OutboxAggregateType;
import com.iprody.inventory.model.OutboxEvent;
import com.iprody.inventory.model.OutboxEventStatus;
import com.iprody.inventory.repository.OutboxEventRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventService {
    private final OutboxEventRepo outboxRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveEvent(OutboxAggregateType aggregateType, UUID aggregateId, OutboxEventType eventType, Object event) {
        try {
            outboxRepo.save(OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .event(objectMapper.writeValueAsString(event))
                    .status(OutboxEventStatus.PENDING)
                    .build());
            log.debug("Outbox event saved: type={}, aggregateId={}, eventType={}",
                    aggregateType, aggregateId, eventType);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event: {}", event, e);
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }

    public boolean existsByAggregateIdAndType(UUID aggregateId, OutboxEventType eventType) {
        return outboxRepo.existsByAggregateIdAndEventType(aggregateId, eventType);
    }
}
