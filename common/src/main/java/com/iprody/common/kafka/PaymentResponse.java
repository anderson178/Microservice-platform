package com.iprody.common.kafka;

import com.iprody.common.struct.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    @NotNull
    private UUID inquiryRefId;
    @NotNull
    private PaymentStatus status;
    private String reason;
}
