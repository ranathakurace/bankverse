package com.bank.api.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AccountAuditResponse {

    private String action;
    private String previousStatus;
    private String newStatus;
    private String performedBy;
    private String reason;
    private LocalDateTime occurredAt;
}
