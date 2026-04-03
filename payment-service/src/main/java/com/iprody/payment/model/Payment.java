package com.iprody.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Data
@Entity
@Table(name = "PAYMENT")
public class Payment {
    private static final int AMOUNT_PRECISION = 5;
    private static final int AMOUNT_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "INQUIRY_REF_ID", nullable = false)
    private UUID inquiryRefId;

    @Column(name = "AMOUNT", nullable = false, precision = AMOUNT_PRECISION, scale = AMOUNT_SCALE)
    private BigDecimal amount;

    @Column(name = "CURRENCY", nullable = false)
    private String currency;

    @Column(name = "TRANSACTION_REF_ID")
    private UUID transactionRefId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private PaymentStatus paymentStatus;

    @Column(name = "NOTE")
    private String note;

    @Column(name = "CREATED_AT", nullable = false)
    private Timestamp createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private Timestamp updatedAt;
}
