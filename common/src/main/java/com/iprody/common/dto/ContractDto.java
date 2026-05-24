package com.iprody.common.dto;

import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;

@Data
public class ContractDto {
    private UUID id;
    private String email;
    private String phoneNumber;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
