package com.iprody.common.dto;

import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;

@Data
public class CustomerDto {
    private UUID id;
    private String fullName;
    private ContractDto contract;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
