package com.bank.api.repository;

import com.bank.api.model.Account;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class AccountRepository {

    private static final Map<Integer, Account> accounts = new HashMap<>();

    static {
    	accounts.put(1,
    		    new Account(1, 101, "SAVINGS", 5000.00, "INR", "ACTIVE"));

    		accounts.put(2,
    		    new Account(2, 102, "CURRENT", 12000.50, "USD", "ACTIVE"));

    		accounts.put(3,
    		    new Account(3, 103, "SAVINGS", 0.00, "INR", "BLOCKED"));
    }

    public Account findById(int accountId) {
        return accounts.get(accountId);
    }
    public void update(Account account) {
        accounts.put(account.getAccountId(), account);
    }

}
