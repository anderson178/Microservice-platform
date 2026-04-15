package com.iprody.inquiry.dto;

import com.iprody.inquiry.model.InquiryStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class InquiryFilterDto {
    private InquiryStatus status;
    private UUID productRefId;
    private UUID customerRefId;
    private UUID managerRefId;
}
