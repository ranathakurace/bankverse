package com.bank.api.dto;

import java.time.LocalDateTime;

import lombok.Data;

/** Response returned after a KYC workflow command. */
@Data
public class KycResponse {

    private Long kycCaseId;
    private String customerNumber;
    private String kycStatus;
    private String message;
    private LocalDateTime processedAt;
}
