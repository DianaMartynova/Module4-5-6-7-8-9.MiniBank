package com.minibank.web;

import com.minibank.repository.BankService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

final class TransferHandler extends AbstractJsonHandler {

    private final BankService bank;

    TransferHandler(BankService bank) {
        this.bank = bank;
    }

    @Override
    protected Object handlePost(HttpExchange exchange) throws IOException {
        TransferRequest req = Json.read(exchange.getRequestBody(), TransferRequest.class);

        if (req.fromAccountId() == null || req.toAccountId() == null) {
            throw new IllegalArgumentException("Нужно указать fromAccountId и toAccountId");
        }
        if (req.amount() == null || req.amount().signum() <= 0) {
            throw new IllegalArgumentException("Сумма перевода должна быть больше нуля");
        }

        bank.transfer(req.fromAccountId(), req.toAccountId(), req.amount());

        return Map.of(
                "status", "ok",
                "fromAccountId", req.fromAccountId(),
                "toAccountId", req.toAccountId(),
                "amount", req.amount());
    }

    record TransferRequest(Long fromAccountId, Long toAccountId, BigDecimal amount) {
    }
}
