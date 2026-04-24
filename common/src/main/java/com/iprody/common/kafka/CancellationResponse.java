package com.iprody.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancellationResponse {
    private UUID id;
    private CancellationStatus status;
    private String reason;
}
