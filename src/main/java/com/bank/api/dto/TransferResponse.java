package com.bank.api.dto;

import java.math.BigDecimal;

public class TransferResponse {

    private String status;
    private String message;
    private BigDecimal fromBalance;
    private BigDecimal toBalance;

    public TransferResponse(
            String status,
            String message,
            BigDecimal fromBalance,
            BigDecimal toBalance) {
        this.status = status;
        this.message = message;
        this.fromBalance = fromBalance;
        this.toBalance = toBalance;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public BigDecimal getFromBalance() {
        return fromBalance;
    }

    public BigDecimal getToBalance() {
        return toBalance;
    }
    
}
