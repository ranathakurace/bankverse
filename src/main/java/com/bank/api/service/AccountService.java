package com.bank.api.service;

import com.bank.api.exception.AccountNotFoundException;
import com.bank.api.model.Account;
import com.bank.api.repository.AccountRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    public Account getAccount(int accountId) {
        Account account = repository.findById(accountId);

        if (account == null) {
            throw new AccountNotFoundException(accountId);
        }

        return account;
    }
}
