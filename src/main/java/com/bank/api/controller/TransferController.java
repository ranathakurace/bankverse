package com.bank.api.controller;

import com.bank.api.dto.TransferRequest;
import com.bank.api.dto.TransferResponse;
import com.bank.api.service.TransferService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@Validated
public class TransferController {

    private final TransferService transferService;

    // Constructor Injection
    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * MONEY TRANSFER API (IDEMPOTENT)
     *
     * Header:
     * Idempotency-Key: unique-client-generated-key
     */
    @PostMapping("/transactions/transfer")
    public TransferResponse transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new RuntimeException("Idempotency-Key header is required");
        }

        return transferService.transfer(idempotencyKey, request);
    }
}
