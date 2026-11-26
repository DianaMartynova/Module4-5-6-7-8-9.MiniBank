package com.minibank.web;

import com.minibank.exception.InsufficientFundsException;
import com.minibank.exception.NotFoundException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

abstract class AbstractJsonHandler implements HttpHandler {

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                writeError(exchange, 405,
                        "Метод " + exchange.getRequestMethod() + " не поддерживается, используйте POST");
                return;
            }
            Object responseBody = handlePost(exchange);
            writeJson(exchange, 200, responseBody);
        } catch (NotFoundException e) {
            writeError(exchange, 404, e.getMessage());
        } catch (InsufficientFundsException e) {
            writeError(exchange, 409, e.getMessage());
        } catch (IllegalArgumentException e) {
            writeError(exchange, 400, e.getMessage());
        } catch (RuntimeException e) {
            writeError(exchange, 500, "Внутренняя ошибка сервера");
        } finally {
            exchange.close();
        }
    }

    protected abstract Object handlePost(HttpExchange exchange) throws IOException;

    private void writeError(HttpExchange exchange, int status, String message) throws IOException {
        writeJson(exchange, status, Map.of("error", message == null ? "" : message));
    }

    private void writeJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = Json.write(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
