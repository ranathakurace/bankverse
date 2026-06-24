package com.bank.api.model;

public class Account {

	private int accountId;
	private int customerId;
	private String accountType;
	private double balance;
	private String currency;
	private String status;

	public Account(int accountId, int customerId, String accountType,
            double balance, String currency, String status) {
		this.accountId = accountId;
		this.customerId = customerId;
		this.accountType = accountType;
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
    public int getCustomerId() {
        return customerId;
    }

    public String getAccountType() {
        return accountType;
    }
}
