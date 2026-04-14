package com.iprody.common.exception;

import com.iprody.common.ResultCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class AppException extends RuntimeException {

    private final ResultCode code;

    public AppException(ResultCode code) {
        super(code.getDefaultMessage());
        this.code = code;
    }

    public AppException(ResultCode code, String message) {
        super(message);
        this.code = code;
    }
}
