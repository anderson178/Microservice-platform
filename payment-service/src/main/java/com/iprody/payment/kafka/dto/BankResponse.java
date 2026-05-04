package com.iprody.payment.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankResponse {
    private UUID id;
    private BigDecimal amount;
    private Currency currency;
    private String customer;
    private UUID orderId;
    private String receiptEmail;
    private Status status;
    private Instant createdAt;
    private Instant chargedAt;

    public void ifPresent(Object errorDuringBankResponseProcessing) {

    }

    public enum Status {
        PROCESSING, CANCELED, SUCCEEDED
    }
}
