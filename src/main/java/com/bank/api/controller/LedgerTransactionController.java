package com.bank.api.controller;

import com.bank.api.dto.FundTransferRequest;
import com.bank.api.dto.FundTransferResponse;
import com.bank.api.dto.LedgerTransactionResponse;
import com.bank.api.dto.MoneyTransactionRequest;
import com.bank.api.dto.TransactionHistoryResponse;
import com.bank.api.service.LedgerTransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class LedgerTransactionController {

    private final LedgerTransactionService ledgerTransactionService;

    public LedgerTransactionController(LedgerTransactionService ledgerTransactionService) {
        this.ledgerTransactionService = ledgerTransactionService;
    }

    @PostMapping("/deposits")
    public ResponseEntity<LedgerTransactionResponse> deposit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody MoneyTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerTransactionService.deposit(idempotencyKey, request));
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<LedgerTransactionResponse> withdraw(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody MoneyTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerTransactionService.withdraw(idempotencyKey, request));
    }

    @PostMapping("/transfers")
    public ResponseEntity<FundTransferResponse> transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody FundTransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerTransactionService.transfer(idempotencyKey, request));
    }

    @GetMapping("/accounts/{accountNumber}")
    public ResponseEntity<TransactionHistoryResponse> getTransactionHistory(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ledgerTransactionService.getTransactionHistory(accountNumber, page, size, type));
    }
}
