package com.bank.api.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MoneyTransactionRequest {

    private String accountNumber;
    private BigDecimal amount;
    private String description;
}
