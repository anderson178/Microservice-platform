package com.iprody.payment.service;

import com.iprody.common.DateRange;
import com.iprody.payment.model.PaymentStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class PaymentFilter {
    private UUID id;
    private UUID inquiryRefId;
    private PaymentStatus status;
    private DateRange dateRange;
}
