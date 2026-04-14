package com.iprody.common.utils;

import org.apache.commons.lang3.StringUtils;

public class EnumUtils {
    /**
     * Converting a string or enum class to an enum with a default value.
     */
    public static <T extends Enum<T>> T getEnum(Object value, Class<T> enumClass, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        try {
            if (value instanceof String) {
                String str = (String) value;
                if (StringUtils.isNoneBlank(str)) {
                    return Enum.valueOf(enumClass, str);
                }
            } else if (value instanceof Enum<?>) {
                Enum<?> e = (Enum<?>) value;
                return Enum.valueOf(enumClass, e.name());
            }
        } catch (IllegalArgumentException ignored) {
        }

        return defaultValue;
    }
}
