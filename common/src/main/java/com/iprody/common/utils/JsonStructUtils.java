package com.iprody.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

public final class JsonStructUtils {
    private final static ObjectMapper jsonMapper;

    private JsonStructUtils() {
    }

    static {
        jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        jsonMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    private static <T> T fromJson(Class<T> t, String data) throws IOException {
        return jsonMapper.readValue(data, t);
    }

    public static <T> T fromJsonSafe(Class<T> t, String data) {
        try {
            return data != null ? fromJson(t, data) : null;
        } catch (Throwable e) {
        }
        return null;
    }
}
