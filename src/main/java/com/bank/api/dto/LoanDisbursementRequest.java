package com.bank.api.dto;

import lombok.Data;

@Data
public class LoanDisbursementRequest {
    private String accountNumber;
    private String performedBy;
    private String reason;
}
