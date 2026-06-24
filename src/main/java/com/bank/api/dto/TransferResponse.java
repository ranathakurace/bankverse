package com.bank.api.dto;

public class TransferResponse {

    private String status;
    private String message;
    private double fromBalance;
    private double toBalance;

    public TransferResponse(String status, String message,
                            double fromBalance, double toBalance) {
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

    public double getFromBalance() {
        return fromBalance;
    }

    public double getToBalance() {
        return toBalance;
    }
}
