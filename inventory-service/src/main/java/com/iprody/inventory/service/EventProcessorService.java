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
        if (CancellationStatus.RECEIVED.equals(request.getStatus())) {
            if (!outboxEventService.existsByAggregateIdAndType(inquiryRefId, OutboxEventType.CANCELLATION_RESPONSE)) {
                CancellationStatus status = groupService.cancellingReservation(request.getId())
                        ? CancellationStatus.SUCCESS
                        : CancellationStatus.FAILED;
                request.setStatus(status);
                saveEvent(inquiryRefId, request, OutboxEventType.CANCELLATION_RESPONSE);
            } else {
                log.info("Cancellation request with aggregationId={} already exists", inquiryRefId);
            }
        } else {
            log.info("Cancellation request is not received status");
        }
    }

    public void processCheckAvailSeats(InventoryAvailabilityRequest request) {
        groupService.checkFindByGroupRefId(request.getGroupRefId());
        InventoryStatus status = groupService.availFreeSeats(request.getGroupRefId(), request.getNumberOfSeats())
                ? InventoryStatus.SEATS_AVAILABLE_FOR_BOOKING
                : InventoryStatus.SEATS_NOT_AVAILABLE_FOR_BOOKING;

        saveEvent(
                request.getInquiryRefId(),
                new InventoryAvailabilityResponse(
                        request.getInquiryRefId(),
                        request.getGroupRefId(),
                        status),
                OutboxEventType.INVENTORY_AVAILABILITY_RESPONSE
        );
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
