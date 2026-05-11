package com.iprody.inventory.service;

import com.iprody.common.kafka.*;
import com.iprody.inventory.model.OutboxAggregateType;
import com.iprody.inventory.model.OutboxEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessorService {
    private final GroupService groupService;
    private final OutboxEventService outboxEventService;


    @Transactional
    public void processCancellationRequest(UUID inquiryRefId, CancellationRequest request) {
        groupService.checkFindByGroupRefId(request.getId());

        if (!CancellationStatus.RECEIVED.equals(request.getStatus())) {
            log.info("Cancellation request {} has invalid status: {}", inquiryRefId, request.getStatus());
            return;
        }

        if (isAlreadyProcessed(inquiryRefId, OutboxEventType.CANCELLATION_RESPONSE)) {
            log.info("Cancellation request with aggregationId={} already processed", inquiryRefId);
            return;
        }

        CancellationStatus status = groupService.cancellingReservation(request.getId())
                ? CancellationStatus.SUCCESS
                : CancellationStatus.FAILED;

        request.setStatus(status);
        saveEvent(inquiryRefId, request, OutboxEventType.CANCELLATION_RESPONSE);
    }

    @Transactional
    public void processReservationRequest(UUID inquiryRefId, InventoryRequest request) {
        groupService.checkFindByGroupRefId(request.getGroupRefId());

        if (isAlreadyProcessed(inquiryRefId, OutboxEventType.INVENTORY_RESPONSE)) {
            log.info("Request is already processed: {}", inquiryRefId);
            return;
        }

        InventoryStatus status = groupService.attemptReservation(request.getGroupRefId(), request.getNumberOfSeats());
        saveEvent(
                inquiryRefId,
                new InventoryResponse(
                        inquiryRefId,
                        request.getGroupRefId(),
                        status,
                        InventoryStatus.ROLLBACK.equals(status) ? "Group Full" : ""),
                OutboxEventType.INVENTORY_RESPONSE);
    }

    private boolean isAlreadyProcessed(UUID inquiryRefId, OutboxEventType eventType) {
        return outboxEventService.existsByAggregateIdAndType(inquiryRefId, eventType);
    }

    private <T> void saveEvent(UUID requestId, T event, OutboxEventType eventType) {
        outboxEventService.saveEvent(
                OutboxAggregateType.GROUP,
                requestId,
                eventType,
                event
        );
    }
}
