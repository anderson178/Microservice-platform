package com.iprody.inquiry.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancellationRequest {
    private UUID id;
    private CancellationStatus status;
    private String reason;
}
