package com.minibank.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

final class Json {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    private Json() {
    }

    static <T> T read(InputStream body, Class<T> type) {
        try {
            T value = MAPPER.readValue(body, type);
            if (value == null) {
                throw new IllegalArgumentException("Тело запроса пустое");
            }
            return value;
        } catch (IOException e) {
            throw new IllegalArgumentException("Некорректный JSON в теле запроса");
        }
    }

    static byte[] write(Object value) {
        try {
            return MAPPER.writeValueAsBytes(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
