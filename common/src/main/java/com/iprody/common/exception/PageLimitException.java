package com.iprody.common.exception;


import com.iprody.common.Pages;

public class PageLimitException extends PageConstraintsException {
    public PageLimitException(Integer limit) {
        super(String.format("Page limit (%s) must be in [%d, %d]",
                limit,
                Pages.MIN_LIMIT,
                Pages.MAX_LIMIT));
    }
}
