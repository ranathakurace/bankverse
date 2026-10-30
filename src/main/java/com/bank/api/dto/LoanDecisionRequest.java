package com.bank.api.dto;

import lombok.Data;

@Data
public class LoanDecisionRequest {
    private String performedBy;
    private String reason;
}
