package com.iprody.inventory.model;

import lombok.Data;

import java.util.UUID;

@Data
public class GroupData {
    private UUID groupRefId;
    private Long currentCount;
    private Long limit;
}
