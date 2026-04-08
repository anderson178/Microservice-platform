package com.iprody.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageUtilsTest {
    private enum TestSortFields implements SortableField {
        FULL_NAME("fullName"),
        CREATED_AT("createdAt");

        private final String attribute;

        TestSortFields(String attribute) {
            this.attribute = attribute;
        }

        @Override
        public String getSortableAttribute() {
            return attribute;
        }
    }

    @Test
    void of_WithPaginationOnly_ShouldReturnPageRequestWithoutSort() {
        Pagination pagination = new Pagination(1, 10);
        PageRequest result = PageUtils.of(pagination);
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertTrue(result.getSort().isUnsorted());
    }

    @Test
    void of_WithPaginationAndSorting_ShouldApplyCorrectSorting() {
        Pagination pagination = new Pagination(0, 10);
        Sorting sorting = new Sorting(TestSortFields.FULL_NAME, Sort.Direction.ASC);
        PageRequest result = PageUtils.of(pagination, sorting);
        assertEquals("fullName",
                Objects.requireNonNull(result.getSort().getOrderFor("fullName")).getProperty());
    }
}