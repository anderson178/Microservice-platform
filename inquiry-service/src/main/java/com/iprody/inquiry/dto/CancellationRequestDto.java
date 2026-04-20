package com.iprody.inquiry.dto;

import com.iprody.inquiry.kafka.CancellationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CancellationRequestDto {
    @NotNull
    private UUID id;
    @NotNull
    private CancellationStatus status;
    private String reason;
}
