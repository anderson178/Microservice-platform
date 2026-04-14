package com.iprody.customer.model;

import com.iprody.common.SortableField;

public enum CustomerSortField implements SortableField {
    FULL_NAME("fullName");

    private final String fieldName;

    CustomerSortField(String name) {
        this.fieldName = name;
    }


    @Override
    public String getSortableAttribute() {
        return fieldName;
    }
}
