package com.bank.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** Account lifecycle statement. Monetary transaction entries are delivered by the later ledger phase. */
@Data
public class AccountStatementResponse {

    private String accountNumber;
    private String customerNumber;
    private String accountType;
    private String accountStatus;
    private String currency;
    private BigDecimal currentBalance;
    private LocalDateTime accountOpenedAt;
    private LocalDateTime statementGeneratedAt;
    private List<AccountAuditResponse> lifecycleEvents;
}
