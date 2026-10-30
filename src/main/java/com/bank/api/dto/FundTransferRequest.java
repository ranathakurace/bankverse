package com.bank.api.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FundTransferRequest {

    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private String description;
}
