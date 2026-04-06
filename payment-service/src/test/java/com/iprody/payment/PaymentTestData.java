package com.iprody.payment;

import com.iprody.payment.model.Payment;
import com.iprody.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentTestData {
    private PaymentTestData() {}

    public static Payment createPayment() {
        Payment payment = new Payment();
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setNote("12345");
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("EUR");
        payment.setInquiryRefId(UUID.randomUUID());
        payment.setTransactionRefId(UUID.randomUUID());
        payment.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        payment.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

        return payment;
    }
}
