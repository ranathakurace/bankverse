package com.bank.api.dto;

public class FraudResponse {

    private boolean fraudulent;
    private String reason;

    public boolean isFraudulent() {
        return fraudulent;
    }

    public void setFraudulent(boolean fraudulent) {
        this.fraudulent = fraudulent;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
