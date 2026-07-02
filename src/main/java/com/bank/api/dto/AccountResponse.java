package com.bank.api.dto;

import java.math.BigDecimal;

public class AccountResponse {

	private Long accountId;
	private BigDecimal balance;
    private String currency;
    private String status;

    public AccountResponse(Long accountId,
            BigDecimal balance,
            String currency,
            String status) {
        this.accountId = accountId;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
    }

    public Long getAccountId() {
        return accountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }
}
