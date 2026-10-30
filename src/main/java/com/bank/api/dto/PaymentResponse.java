package com.bank.api.dto;
import java.math.BigDecimal;
import lombok.Data;
@Data public class PaymentResponse { private String paymentReference; private String sourceAccountNumber; private String method; private String status; private BigDecimal amount; private String currency; private String beneficiaryReference; private Integer attemptCount; }
