package com.bank.api.dto;

import java.math.BigDecimal;

import com.bank.api.model.AccountStatus;
import com.bank.api.model.AccountType;

public class AccountResponse {

	private String accountNumber;
	private String customerNumber;
	private AccountType accountType;
	private BigDecimal balance;
	private String currency;
	private AccountStatus accountStatus;
	
	public AccountResponse(String accountNumber,
            String customerNumber,
            AccountType accountType,
            BigDecimal balance,
            String currency,
            AccountStatus accountStatus) {

		this.accountNumber = accountNumber;
		this.customerNumber = customerNumber;
		this.accountType = accountType;
		this.balance = balance;
		this.currency = currency;
		this.accountStatus = accountStatus;
		}

	public String getAccountNumber() {
	    return accountNumber;
	}

	public String getCustomerNumber() {
	    return customerNumber;
	}

	public AccountType getAccountType() {
	    return accountType;
	}

	public AccountStatus getAccountStatus() {
	    return accountStatus;
	}
    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    
}
