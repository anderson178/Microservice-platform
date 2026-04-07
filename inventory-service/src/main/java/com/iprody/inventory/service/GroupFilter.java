package com.iprody.inventory.service;

import lombok.Data;

import java.util.UUID;

@Data
public class GroupFilter {
    private UUID groupRefId;
    private Boolean isAvailFreePlaces;
}
