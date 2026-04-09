package com.iprody.common.dto;

import lombok.Data;

@Data
public class PaginationDto {
    private Long offset;
    private int limit;
}
