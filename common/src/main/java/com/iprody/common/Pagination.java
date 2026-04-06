package com.iprody.common;

import lombok.Data;

@Data
public class Pagination {
    private Integer offset;
    private Integer limit;

    public Pagination(Integer offset, Integer limit) {
        this.offset = Pages.convertOffset(offset);
        this.limit = Pages.convertLimit(limit);
    }
}
