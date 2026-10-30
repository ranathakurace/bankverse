package com.bank.api.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FundTransferResponse {

    private String reference;
    private String status;
    private String sourceAccountNumber;
    private BigDecimal sourceBalanceAfter;
    private String destinationAccountNumber;
    private BigDecimal destinationBalanceAfter;
    private String currency;
}
