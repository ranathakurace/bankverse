package com.bank.api.controller;

import com.bank.api.client.FraudClient;
import com.bank.api.dto.AccountRequest;
import com.bank.api.dto.AccountResponse;
import com.bank.api.dto.FraudResponse;
import com.bank.api.model.Account;
import com.bank.api.service.AccountService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
            Long accountId) {

        // 🔍 External Fraud Check (WireMock)
    	FraudResponse fraudResponse = fraudClient.checkFraud(accountId);

        if (fraudResponse != null && fraudResponse.isFraudulent()) {
            throw new RuntimeException(
                    "Account flagged for fraud: " + fraudResponse.getReason()
            );
        }
       
        Account account = accountService.getAccount(accountId);

        return new AccountResponse(
                account.getAccountNumber(),
                account.getCustomer().getCustomerNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency(),
                account.getAccountStatus()
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

    	accountService.deleteAccount((long) accountId);
    	return "Account deleted successfully";
    }
    
    /**
     * ==========================================================
     * Story-003
     * Create Bank Account
     * ==========================================================
     */
    @PostMapping("/bank/account")
    public ResponseEntity<AccountResponse> createAccount(
            @RequestBody AccountRequest request) {

        AccountResponse response =
                accountService.createAccount(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
