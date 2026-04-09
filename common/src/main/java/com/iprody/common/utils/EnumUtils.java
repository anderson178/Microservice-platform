package com.iprody.common.utils;

import org.apache.commons.lang3.StringUtils;

public class EnumUtils {
    public static <T extends Enum<T>> T getEnum(Object rawValue, Class<T> enumClass, T defaultValue) {
        if (rawValue == null) {
            return defaultValue;
        }

        try {
            if (rawValue instanceof String) {
                String str = (String) rawValue;
                if (StringUtils.isNoneBlank(str)) {
                    return Enum.valueOf(enumClass, str);
                }
            } else if (rawValue instanceof Enum<?>) {
                Enum<?> e = (Enum<?>) rawValue;
                return Enum.valueOf(enumClass, e.name());
            }
        } catch (IllegalArgumentException ignored) {
        }

        return defaultValue;
    }
}
