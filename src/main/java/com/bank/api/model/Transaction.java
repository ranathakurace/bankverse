package com.bank.api.model;

import java.time.LocalDateTime;

public class Transaction {

    private int transactionId;
    private int accountId;
    private String type; // DEPOSIT / WITHDRAW
    private double amount;
    private LocalDateTime timestamp;

    public Transaction(int transactionId, int accountId, String type, double amount) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public int getTransactionId() {
        return transactionId;
    }

    public int getAccountId() {
        return accountId;
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
