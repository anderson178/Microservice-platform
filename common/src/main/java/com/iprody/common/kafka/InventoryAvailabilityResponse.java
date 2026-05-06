package com.iprody.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAvailabilityResponse {
    private UUID inquiryRefId;
    private UUID groupRefId;
    private InventoryStatus status;
}
