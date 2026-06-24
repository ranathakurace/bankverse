package com.bank.api.dto;

public class AccountResponse {

    private int accountId;
    private double balance;
    private String currency;
    private String status;

    public AccountResponse(int accountId, double balance, String currency, String status) {
        this.accountId = accountId;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
    }

    public int getAccountId() {
        return accountId;
    }

    public double getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }
}
