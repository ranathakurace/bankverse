package com.bank.api.exception;

public class InvalidKycException extends RuntimeException {

    public InvalidKycException(String message) {
        super(message);
    }
}
