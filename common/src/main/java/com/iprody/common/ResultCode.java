package com.iprody.common;

import java.util.Objects;

public enum ResultCode {
    PAGE_CONSTRAINTS_ARE_NOT_SATISFIED("Check documentation about paging constraints");

    private final String defaultMessage;

    ResultCode(String defaultMessage) {
        this.defaultMessage = Objects.requireNonNull(defaultMessage, "`defaultMessage` required");
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
