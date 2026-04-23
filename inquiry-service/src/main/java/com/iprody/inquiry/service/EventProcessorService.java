package com.iprody.inquiry.service;

import com.iprody.inquiry.kafka.CancellationEventPublisher;
import com.iprody.inquiry.kafka.CancellationRequest;
import com.iprody.inquiry.kafka.CancellationResponse;
import com.iprody.inquiry.kafka.CancellationStatus;
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

        inquiry.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        inquiryService.update(inquiry.getId(), InquiryMapper.INSTANCE.update(inquiry));

        log.info("Updated inquiry status: id={}, newStatus={}", inquiry.getId(), inquiry.getStatus());
    }
}
