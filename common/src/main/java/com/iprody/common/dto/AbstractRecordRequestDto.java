package com.iprody.common.dto;

import lombok.Data;

@Data
public class AbstractRecordRequestDto<T> {
    private PaginationDto pagination;
    private SortingDto<T> sorting;

    public AbstractRecordRequestDto() {
        this.pagination = new PaginationDto();
        this.sorting = new SortingDto<>();
    }
}
