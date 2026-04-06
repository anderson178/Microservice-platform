package com.iprody.common.exception;


import com.iprody.common.ResultCode;

public class PageConstraintsException extends AppException {

    public PageConstraintsException(String message) {
        super(ResultCode.PAGE_CONSTRAINTS_ARE_NOT_SATISFIED, message);
    }
}
