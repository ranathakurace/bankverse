package com.bank.api.dto;

import lombok.Data;

/** Input captured when a compliance reviewer starts a submitted KYC case. */
@Data
public class KycReviewStartRequest {

    private String reviewedBy;
    private String remarks;
}
