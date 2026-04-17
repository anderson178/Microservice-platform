package com.iprody.common.exception;

import com.iprody.common.ResultCode;
import lombok.Getter;

import java.util.UUID;

@Getter
public class AppException extends RuntimeException {
    private final UUID resourceId;
    private final ResultCode code;

    public AppException(ResultCode code, UUID resourceId) {
        super(code.getDefaultMessage());
        this.resourceId = resourceId;
        this.code = code;
    }

    public AppException(ResultCode code, String message) {
        super(message);
        this.code = code;
        this.resourceId = null;
    }
}
