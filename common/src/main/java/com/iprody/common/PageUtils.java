package com.iprody.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PageUtils {
    private PageUtils() {
    }

    public static PageRequest of(Pagination pagination, Sorting sorting) {
        return PageRequest.of(
                pagination.getOffset(),
                pagination.getLimit(),
                Sort.by(sorting.getSortDirection(), ((SortableField) sorting.getSortField()).getSortableAttribute())
        );
    }
}
