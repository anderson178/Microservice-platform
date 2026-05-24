package com.iprody.inquiry.service;

import com.iprody.common.ResultCode;
import com.iprody.common.dto.CustomerDto;
import com.iprody.common.exception.AppException;
import com.iprody.common.utils.JsonStructUtils;
import com.iprody.inquiry.model.Inquiry;
import com.iprody.inquiry.model.InquiryData;
import com.iprody.inquiry.model.OutboxAggregateType;
import com.iprody.inquiry.model.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InquiryFacade {
    private final InquiryService inquiryService;
    private final HTTPCustomerService httpCustomerService;
    private final OutboxEventService outboxEventService;

    @Transactional
    public Inquiry save(InquiryData data, String tokenAuthentication) {
        if (!existCustomer(data.getCustomerRefId(), tokenAuthentication)) {
            throw new AppException(ResultCode.NOT_FOUND, data.getCustomerRefId());
        }

        Inquiry inquirySaved = inquiryService.save(data);

        outboxEventService.saveEvent(
                OutboxAggregateType.INQUIRY,
                inquirySaved.getId(),
                OutboxEventType.PAYMENT_REQUEST,
                inquirySaved);

        return inquirySaved;
    }

    private boolean existCustomer(UUID customerRefId, String tokenAuthentication) {
        ResponseEntity<String> response = httpCustomerService.getById(customerRefId, tokenAuthentication);

        if (response == null) {
            throw new AppException(ResultCode.EXTERNAL_SERVICE_UNAVAILABLE, "Customer service returned empty response");
        }

        if (response.getStatusCode().value() == 404) {
            throw new AppException(ResultCode.NOT_FOUND, customerRefId);
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AppException(ResultCode.EXTERNAL_SERVICE_UNAVAILABLE, "Customer service error status: " + response.getStatusCode());
        }

        String body = response.getBody();
        if (StringUtils.isBlank(body)) {
            throw new AppException(ResultCode.NOT_FOUND, customerRefId);
        }

        return JsonStructUtils.fromJsonSafe(CustomerDto.class, body) != null;
    }
}
