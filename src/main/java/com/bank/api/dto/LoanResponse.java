package com.bank.api.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class LoanResponse {
    private String loanReference;
    private String customerNumber;
    private String disbursementAccountNumber;
    private String loanType;
    private String status;
    private BigDecimal principal;
    private BigDecimal annualInterestRate;
    private Integer termMonths;
    private BigDecimal remainingPrincipal;
}
