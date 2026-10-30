package com.bank.api.dto;
import java.math.BigDecimal;
import lombok.Data;
@Data public class RiskAssessmentRequest { private String accountNumber; private String operationType; private String operationReference; private BigDecimal amount; }
