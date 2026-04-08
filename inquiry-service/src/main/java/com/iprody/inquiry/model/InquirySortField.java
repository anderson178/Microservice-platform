package com.iprody.inquiry.model;

import com.iprody.common.SortableField;

public enum InquirySortField implements SortableField {
    STATUS("paymentStatus"),
    CREATED_AT("createdAt");

    private final String fieldName;

    InquirySortField(String name) {
        this.fieldName = name;
    }


    @Override
    public String getSortableAttribute() {
        return fieldName;
    }
}
