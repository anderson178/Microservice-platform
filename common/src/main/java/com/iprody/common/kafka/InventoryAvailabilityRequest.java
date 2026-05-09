package com.iprody.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryAvailabilityRequest {
    private UUID inquiryRefId;
    private UUID groupRefId;
    private Long numberOfSeats;
}
