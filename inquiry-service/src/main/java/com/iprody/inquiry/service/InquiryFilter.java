package com.iprody.inquiry.service;

import com.iprody.inquiry.model.InquiryStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class InquiryFilter {
    private InquiryStatus status;
    private UUID customerRefId;
    private UUID managerRefId;
}
