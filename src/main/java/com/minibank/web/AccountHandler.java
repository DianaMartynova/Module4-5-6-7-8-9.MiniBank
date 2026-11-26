package com.minibank.web;

import com.minibank.model.Account;
import com.minibank.repository.BankService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

final class AccountHandler extends AbstractJsonHandler {

    private final BankService bank;

    AccountHandler(BankService bank) {
        this.bank = bank;
    }

    @Override
    protected Object handleDelete(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.startsWith("id=")) {
            throw new IllegalArgumentException("Нужно указать ?id=номер счёта");
        }
        long id = Long.parseLong(query.substring(3));
        bank.removeAccount(id);
        return Map.of("status", "deleted", "id", id);
    }

    @Override
    protected Object handlePost(HttpExchange exchange) throws IOException {
        AccountRequest req = Json.read(exchange.getRequestBody(), AccountRequest.class);

        if (req.clientId() == null || isBlank(req.accountNumber())) {
            throw new IllegalArgumentException("Нужно указать clientId и accountNumber");
        }

        Account account = bank.openAccount(req.clientId(), req.accountNumber().trim());

        return Map.of(
                "id", account.getId(),
                "clientId", account.getClientId(),
                "accountNumber", account.getAccountNumber(),
                "balance", account.getBalance());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    record AccountRequest(Long clientId, String accountNumber) {
    }
}
