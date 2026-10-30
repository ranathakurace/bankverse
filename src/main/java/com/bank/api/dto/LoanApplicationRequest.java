package com.bank.api.dto;

import com.bank.api.model.LoanType;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class LoanApplicationRequest {
    private String customerNumber;
    private LoanType loanType;
    private BigDecimal principal;
    private BigDecimal annualInterestRate;
    private Integer termMonths;
}
