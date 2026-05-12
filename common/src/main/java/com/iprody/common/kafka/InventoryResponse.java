package com.iprody.common.kafka;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    @NotNull
    private UUID inquiryRefId;
    private UUID groupRefId;
    @NotNull
    private InventoryStatus status;
    private String reason;
}
