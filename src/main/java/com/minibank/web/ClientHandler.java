package com.minibank.web;

import com.minibank.model.Client;
import com.minibank.repository.BankService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

final class ClientHandler extends AbstractJsonHandler {

    private final BankService bank;

    ClientHandler(BankService bank) {
        this.bank = bank;
    }

    @Override
    protected Object handlePost(HttpExchange exchange) throws IOException {
        ClientRequest req = Json.read(exchange.getRequestBody(), ClientRequest.class);

        if (isBlank(req.name()) || isBlank(req.email())) {
            throw new IllegalArgumentException("Нужно указать name и email");
        }

        Client client = bank.registerClient(req.name().trim(), req.email().trim());

        return Map.of(
                "id", client.getId(),
                "name", client.getName(),
                "email", client.getEmail());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    record ClientRequest(String name, String email) {
    }
}
