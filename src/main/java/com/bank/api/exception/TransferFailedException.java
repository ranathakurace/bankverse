package com.bank.api.exception;

public class TransferFailedException extends RuntimeException {

    public TransferFailedException(String message) {
        super(message);
    }
}
