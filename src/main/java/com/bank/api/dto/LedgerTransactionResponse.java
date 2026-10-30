package com.bank.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class LedgerTransactionResponse {

    private String reference;
    private String accountNumber;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String currency;
    private String counterpartyAccountNumber;
    private String description;
    private LocalDateTime occurredAt;
}
