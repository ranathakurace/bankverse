package com.bank.api.dto;
import com.bank.api.model.PaymentMethod;
import java.math.BigDecimal;
import lombok.Data;
@Data public class PaymentInitiationRequest { private String sourceAccountNumber; private PaymentMethod method; private BigDecimal amount; private String beneficiaryReference; }
