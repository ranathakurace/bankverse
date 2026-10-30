package com.bank.api.dto;

import com.bank.api.model.KycDecision;

import lombok.Data;

/** Input captured when a compliance reviewer approves or rejects a KYC case. */
@Data
public class KycReviewRequest {

    private KycDecision decision;
    private String reviewedBy;
    private String remarks;
}
