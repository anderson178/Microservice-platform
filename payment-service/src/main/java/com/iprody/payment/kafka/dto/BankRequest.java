package com.iprody.payment.kafka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankRequest {
    @NotNull
    private UUID inquiryRefId;
    @NotNull
    private UUID paymentRefId;
    @NotNull
    private BigDecimal amount;
    @NotBlank
    private String currency;
}
