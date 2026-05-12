package com.iprody.inquiry.service;

import com.iprody.common.kafka.CancellationRequest;
import com.iprody.common.kafka.CancellationResponse;
import com.iprody.common.kafka.CancellationStatus;
import com.iprody.common.kafka.PaymentResponse;
import com.iprody.common.struct.PaymentStatus;
import com.iprody.inquiry.mapper.InquiryMapper;
import com.iprody.inquiry.model.Inquiry;
import com.iprody.inquiry.model.InquiryStatus;
import com.iprody.inquiry.model.OutboxAggregateType;
import com.iprody.inquiry.model.OutboxEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessorService {
    private final InquiryService inquiryService;
    private final OutboxEventService outboxEventService;

    public void processCancellationRequest(CancellationRequest request) {
        inquiryService.checkById(request.getId());
        saveEvent(request.getId(), request, OutboxEventType.CANCELLATION_REQUEST);
    }

    @Transactional
    public void processCancellationResponse(UUID inquiryRefId, CancellationResponse response) {
        Inquiry inquiry = inquiryService.findById(inquiryRefId);

        if (CancellationStatus.SUCCESS.equals(response.getStatus())) {
            inquiry.setStatus(InquiryStatus.CANCELLED);
            inquiry.setNote("Cancelled via external service. Reason: "
                    + (StringUtils.isNoneBlank(response.getReason()) ? response.getReason() : "N/A"));
        } else {
            inquiry.setNote("Cancellation rejected by external service: " + response.getReason());
        }

        updateProcess(inquiry);
    }

    @Transactional
    public void processPaymentResponse(UUID inquiryRefId, PaymentResponse response) {
        Inquiry inquiry = inquiryService.findById(inquiryRefId);

        if (PaymentStatus.RECEIVED.equals(response.getStatus())) {
            inquiry.setStatus(InquiryStatus.PAYMENT);
        } else {
            inquiry.setNote("Payment rejected by external service: " + response.getReason());
        }

        updateProcess(inquiry);
    }

    private void updateProcess(Inquiry inquiry) {
        inquiry.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        inquiryService.update(inquiry.getId(), InquiryMapper.INSTANCE.update(inquiry));

        log.info("Updated inquiry status: id={}, newStatus={}", inquiry.getId(), inquiry.getStatus());
    }

    private <T> void saveEvent(UUID requestId, T event, OutboxEventType eventType) {
        outboxEventService.saveEvent(
                OutboxAggregateType.INQUIRY,
                requestId,
                eventType,
                event
        );
    }
}
