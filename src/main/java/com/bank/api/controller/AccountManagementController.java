package com.bank.api.controller;

import com.bank.api.dto.AccountRequest;
import com.bank.api.dto.AccountResponse;
import com.bank.api.dto.AccountStatementResponse;
import com.bank.api.dto.AccountStatusChangeRequest;
import com.bank.api.service.AccountService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountManagementController {

    private final AccountService accountService;

    public AccountManagementController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> openAccount(@RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByAccountNumber(accountNumber));
    }

    @GetMapping("/customer/{customerNumber}")
    public ResponseEntity<List<AccountResponse>> getCustomerAccounts(@PathVariable String customerNumber) {
        return ResponseEntity.ok(accountService.getAccountsByCustomerNumber(customerNumber));
    }

    @PostMapping("/{accountNumber}/freeze")
    public ResponseEntity<AccountResponse> freezeAccount(@PathVariable String accountNumber,
            @RequestBody AccountStatusChangeRequest request) {
        return ResponseEntity.ok(accountService.freezeAccount(accountNumber, request));
    }

    @PostMapping("/{accountNumber}/unfreeze")
    public ResponseEntity<AccountResponse> unfreezeAccount(@PathVariable String accountNumber,
            @RequestBody AccountStatusChangeRequest request) {
        return ResponseEntity.ok(accountService.unfreezeAccount(accountNumber, request));
    }

    @PostMapping("/{accountNumber}/close")
    public ResponseEntity<AccountResponse> closeAccount(@PathVariable String accountNumber,
            @RequestBody AccountStatusChangeRequest request) {
        return ResponseEntity.ok(accountService.closeAccount(accountNumber, request));
    }

    @GetMapping("/{accountNumber}/statement")
    public ResponseEntity<AccountStatementResponse> getStatement(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountStatement(accountNumber));
    }
}
