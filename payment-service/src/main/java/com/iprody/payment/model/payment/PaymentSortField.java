package com.iprody.payment.model.payment;

import com.iprody.common.SortableField;

public enum PaymentSortField implements SortableField {
    STATUS("paymentStatus"),
    CREATED_AT("createdAt");

    private final String fieldName;

    PaymentSortField(String name) {
        this.fieldName = name;
    }


    @Override
    public String getSortableAttribute() {
        return fieldName;
    }
}
