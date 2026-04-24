package com.iprody.common.utils;

import java.util.UUID;

public final class UUIDUtils {
    private UUIDUtils() {
    }

    public static boolean isUUID(String str) {
        if (str == null) {
            return false;
        }

        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
