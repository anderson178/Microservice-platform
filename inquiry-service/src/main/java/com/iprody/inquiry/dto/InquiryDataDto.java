package com.iprody.inquiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class InquiryDataDto {
    @NotNull
    private UUID groupRefId;
    @NotNull
    private UUID customerRefId;
    @NotNull
    private UUID managerRefId;
    @NotBlank
    private String source;
    @NotNull
    @Positive
    private Long numberOfSeats;
}
