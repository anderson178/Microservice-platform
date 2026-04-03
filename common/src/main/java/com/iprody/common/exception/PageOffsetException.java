package com.iprody.common.exception;


import com.iprody.common.Pages;

public class PageOffsetException extends PageConstraintsException {

    public PageOffsetException(Integer offset) {
        super(String.format("Page offset (%s) must be in [%d, %d]",
                offset,
                Pages.MIN_OFFSET,
                Pages.MAX_OFFSET));
    }
}
