package com.iprody.common;

import com.iprody.common.exception.PageLimitException;
import com.iprody.common.exception.PageOffsetException;

public final class Pages {
    public static final int MIN_OFFSET = 0;
    public static final int DEFAULT_OFFSET = 0;
    public static final int MAX_OFFSET = Integer.MAX_VALUE;
    public static final int MIN_LIMIT = 1;
    public static final int MAX_LIMIT = 10;

    private Pages() {
    }

    public static int convertOffset(Integer offsetInRows) {
        if (offsetInRows != null && (offsetInRows < MIN_OFFSET)) {
            throw new PageOffsetException(offsetInRows);
        }
        return offsetInRows != null ? offsetInRows : DEFAULT_OFFSET;
    }

    public static int convertLimit(Integer pageSize) {
        if (pageSize != null && (pageSize < MIN_LIMIT || pageSize > MAX_LIMIT)) {
            throw new PageLimitException(pageSize);
        }
        return pageSize != null ? pageSize : MAX_LIMIT;
    }
}
