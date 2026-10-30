package com.bank.api.dto;

import lombok.Data;

/** Input captured when an operations user submits a KYC case for review. */
@Data
public class KycSubmissionRequest {

    private String documentReference;
    private String submittedBy;
    private String remarks;
}
