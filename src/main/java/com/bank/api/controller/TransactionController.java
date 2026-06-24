package com.bank.api.controller;

import com.bank.api.dto.TransactionResponse;
import com.bank.api.model.Transaction;
import com.bank.api.service.TransactionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Validated
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> getTransactions(
            @RequestParam int accountId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) int size,
            @RequestParam(required = false) String type) {

        List<Transaction> transactions =
                service.getTransactions(accountId, page, size, type);

        return transactions.stream()
                .map(t -> new TransactionResponse(
                        t.getTransactionId(),
                        t.getType(),
                        t.getAmount(),
                        t.getTimestamp()))
                .collect(Collectors.toList());
    }
}
