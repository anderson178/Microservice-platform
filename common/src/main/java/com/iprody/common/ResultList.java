package com.iprody.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
public class ResultList<T> {
    private List<T> elements;
    private long totalCount;

    public static <T> ResultList<T> from(Page<T> page) {
        return new ResultList<>(page.getContent(), page.getTotalElements());
    }
}
