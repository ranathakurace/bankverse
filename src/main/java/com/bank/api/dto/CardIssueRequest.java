package com.bank.api.dto;

import com.bank.api.model.CardType;
import lombok.Data;

@Data
public class CardIssueRequest {
    private String customerNumber;
    private String linkedAccountNumber;
    private CardType cardType;
}
