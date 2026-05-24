package com.iprody.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Objects;

@Getter
public enum ResultCode {
    PAGE_CONSTRAINTS_ARE_NOT_SATISFIED("Check documentation about paging constraints", HttpStatus.BAD_REQUEST),
    NOT_FOUND("Entity not found", HttpStatus.NOT_FOUND),
    CUSTOMER_NOT_FOUND("Customer not found", HttpStatus.NOT_FOUND),
    INCORRECT_PARAMS("Params is incorrected", HttpStatus.BAD_REQUEST),
    BAD_REQUEST("Bad request", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR("Validation error", HttpStatus.BAD_REQUEST),
    EXTERNAL_SERVICE_UNAVAILABLE("Dependent service is currently unavailable", HttpStatus.FAILED_DEPENDENCY);

    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ResultCode(String defaultMessage, HttpStatus httpStatus) {
        this.defaultMessage = Objects.requireNonNull(defaultMessage, "`defaultMessage` required");
        this.httpStatus = httpStatus;
    }
}
