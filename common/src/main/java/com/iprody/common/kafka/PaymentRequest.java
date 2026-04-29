package com.iprody.common.kafka;

import com.iprody.common.struct.PaymentStatus;
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
public class PaymentRequest {
    @NotNull
    private UUID inquiryRefId;
    @NotNull
    private BigDecimal amount;
    @NotBlank
    private String currency;
    private PaymentStatus status;
    private String note;
}
