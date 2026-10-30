package com.bank.api.dto;
import lombok.Data;
@Data public class RiskAssessmentResponse { private String assessmentReference; private String accountNumber; private Integer riskScore; private String decision; private String reason; private String controlCaseReference; }
