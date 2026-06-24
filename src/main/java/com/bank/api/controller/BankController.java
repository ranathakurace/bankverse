package com.bank.api.controller;

import com.bank.api.client.FraudClient;
import com.bank.api.dto.AccountResponse;
import com.bank.api.dto.FraudResponse;
import com.bank.api.model.Account;
import com.bank.api.service.AccountService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;

@RestController
@Validated
public class BankController {

    private final AccountService accountService;
    private final FraudClient fraudClient;

    // Constructor Injection
    public BankController(AccountService accountService, FraudClient fraudClient) {
        this.accountService = accountService;
        this.fraudClient = fraudClient;
    }

    /**
     * USER + ADMIN
     * Get account balance (with fraud check)
     */
    @GetMapping("/bank/balance/{accountId}")
    public AccountResponse getBalance(
            @PathVariable
            @Min(value = 1, message = "accountId must be greater than 0")
            int accountId) {

        // 🔍 External Fraud Check (WireMock)
        FraudResponse fraudResponse = fraudClient.checkFraud(accountId);

        if (fraudResponse != null && fraudResponse.isFraudulent()) {
            throw new RuntimeException(
                    "Account flagged for fraud: " + fraudResponse.getReason()
            );
        }

        Account account = accountService.getAccount(accountId);

        return new AccountResponse(
                account.getAccountId(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus()
        );
    }

    /**
     * ADMIN ONLY
     * Delete account
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/bank/account/{accountId}")
    public String deleteAccount(
            @PathVariable
            @Min(value = 1, message = "accountId must be greater than 0")
            int accountId) {

        return "Account " + accountId + " deleted successfully";
    }
}
