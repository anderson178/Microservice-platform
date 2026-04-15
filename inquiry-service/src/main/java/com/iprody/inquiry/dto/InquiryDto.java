package com.iprody.inquiry.dto;

import com.iprody.inquiry.model.InquiryStatus;
import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;

@Data
public class InquiryDto {
    private UUID id;
    private UUID productRefId;
    private UUID customerRefId;
    private UUID groupRefId;
    private UUID managerRefId;
    private String source;
    private String comment;
    private InquiryStatus status;
    private String note;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
