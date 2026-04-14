package com.iprody.inquiry.model;

import lombok.Data;

import java.util.UUID;

@Data
public class InquiryUpdateData {
    private InquiryStatus status;
    private UUID managerRefId;
}
