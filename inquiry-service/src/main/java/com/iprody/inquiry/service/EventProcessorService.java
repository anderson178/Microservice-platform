package com.iprody.inquiry.service;

import com.iprody.common.kafka.CancellationRequest;
import com.iprody.common.kafka.CancellationResponse;
import com.iprody.common.kafka.CancellationStatus;
import com.iprody.common.kafka.PaymentResponse;
import com.iprody.common.struct.PaymentStatus;
import com.iprody.inquiry.kafka.event.CancellationEventPublisher;
import com.iprody.inquiry.mapper.InquiryMapper;
import com.iprody.inquiry.model.Inquiry;
import com.iprody.inquiry.model.InquiryStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessorService {
    private final InquiryService inquiryService;
    private final CancellationEventPublisher cancellationEventPublisher;

    public void processCancellationRequest(CancellationRequest cancellationRequest) {
        inquiryService.checkById(cancellationRequest.getId());
        cancellationEventPublisher.publish(cancellationRequest);
    }

    @Transactional
    public void processCancellationResponse(CancellationResponse response) {
        Inquiry inquiry = inquiryService.findById(response.getId());

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
    public void processPaymentResponse(PaymentResponse response) {
        Inquiry inquiry = inquiryService.findById(response.getInquiryRefId());

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
}
