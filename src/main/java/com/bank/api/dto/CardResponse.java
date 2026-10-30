package com.bank.api.dto;

import lombok.Data;

@Data
public class CardResponse {
    private String cardReference;
    private String maskedCardNumber;
    private String customerNumber;
    private String linkedAccountNumber;
    private String cardType;
    private String status;
}
