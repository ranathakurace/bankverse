package com.bank.api.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class TransferRequest {

    @NotNull
    private Integer fromAccountId;

    @NotNull
    private Integer toAccountId;

    @Min(value = 1, message = "Transfer amount must be greater than 0")
    private double amount;

    public Integer getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(Integer fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public Integer getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(Integer toAccountId) {
        this.toAccountId = toAccountId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
