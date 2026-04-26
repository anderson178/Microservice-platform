package com.iprody.inventory.service;

import com.iprody.common.kafka.CancellationRequest;
import com.iprody.common.kafka.CancellationStatus;
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
    public void processCancellationRequest(UUID requestId, CancellationRequest request) {
        groupService.checkFindByGroupRefId(request.getId());
        if (CancellationStatus.RECEIVED.equals(request.getStatus())) {
            if (!outboxEventService.existsByAggregateIdAndType(requestId, OutboxEventType.CANCELLATION_REQUESTED)) {
                CancellationStatus status = groupService.cancellingReservation(request.getId())
                        ? CancellationStatus.SUCCESS
                        : CancellationStatus.FAILED;
                saveEvent(requestId, status, request);
            } else {
                log.info("Cancellation request with aggregationId={} already exists", requestId);
            }
        } else {
            log.info("Cancellation request is not received status");
        }
    }

    private void saveEvent(UUID requestId, CancellationStatus status, CancellationRequest request) {
        request.setStatus(status);
        outboxEventService.saveEvent(
                OutboxAggregateType.GROUP,
                requestId,
                OutboxEventType.CANCELLATION_REQUESTED,
                request
        );
    }
}
