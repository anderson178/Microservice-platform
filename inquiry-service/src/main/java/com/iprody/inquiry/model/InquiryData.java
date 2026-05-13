package com.iprody.inquiry.model;

import lombok.Data;

import java.util.UUID;

@Data
public class InquiryData {
    private UUID groupRefId;
    private UUID customerRefId;
    private UUID managerRefId;
    private String source;
    private Long numberOfSeats;
}
