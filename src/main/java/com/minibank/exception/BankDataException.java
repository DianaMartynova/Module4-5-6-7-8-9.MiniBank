package com.minibank.exception;

public class BankDataException extends RuntimeException {
    public BankDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
