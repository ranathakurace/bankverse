package com.bank.api.exception;

public class TransactionWorkflowException extends RuntimeException {

    public TransactionWorkflowException(String message) {
        super(message);
    }
}
