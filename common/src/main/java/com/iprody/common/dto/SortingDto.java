package com.iprody.common.dto;

import lombok.Data;

@Data
public class SortingDto<T> {
    private T sortField;
    private SortDirectionTypeDto sortDirection;
}
