package com.iprody.common.kafka;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancellationRequest {
    @NotNull
    private UUID id;
    @NotNull
    private CancellationStatus status;
    private String reason;
}
