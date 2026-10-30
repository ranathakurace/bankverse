package com.bank.api.dto;

import java.time.LocalDateTime;

import lombok.Data;

/** Read-only entry in the KYC audit history. */
@Data
public class KycAuditResponse {

    private String action;
    private String status;
    private String performedBy;
    private String remarks;
    private LocalDateTime occurredAt;
}
