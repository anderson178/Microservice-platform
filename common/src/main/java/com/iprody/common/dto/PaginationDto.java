package com.iprody.common.dto;

import lombok.Data;

@Data
public class PaginationDto {
    private Long offset = 0L;
    private int limit = 10;
}
