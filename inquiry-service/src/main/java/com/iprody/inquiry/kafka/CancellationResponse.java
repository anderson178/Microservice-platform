package com.iprody.inquiry.kafka;

import lombok.Data;

import java.util.UUID;

@Data
public class CancellationResponse {
    private UUID id;
    private CancellationStatus status;
    private String reason;
}
