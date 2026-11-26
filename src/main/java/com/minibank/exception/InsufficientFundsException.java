package com.minibank.exception;

public class InsufficientFundsException extends IllegalStateException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
