package com.iprody.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Sort;

@Data
@AllArgsConstructor
public class Sorting {
    private Enum<? extends SortableField> sortField;
    private Sort.Direction sortDirection;
}
