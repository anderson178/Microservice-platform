package com.iprody.common;

import lombok.Getter;

import java.util.Objects;

@Getter
public enum ResultCode {
    PAGE_CONSTRAINTS_ARE_NOT_SATISFIED("Check documentation about paging constraints"),
    NOT_FOUND("Entity not found"),
    INCORRECT_PARAMS("Params is incorrected")
    ;

    private final String defaultMessage;

    ResultCode(String defaultMessage) {
        this.defaultMessage = Objects.requireNonNull(defaultMessage, "`defaultMessage` required");
    }
}
