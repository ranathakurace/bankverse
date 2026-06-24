package com.bank.api.dto;

import java.time.LocalDateTime;

public class TransactionResponse {

    private int transactionId;
    private String type;
    private double amount;
    private LocalDateTime timestamp;

    public TransactionResponse(int transactionId, String type, double amount, LocalDateTime timestamp) {
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
