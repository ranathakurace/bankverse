package com.bank.api.model;

/** Lifecycle states for a customer's KYC verification. */
public enum KycStatus {
    PENDING,
    SUBMITTED,
    UNDER_REVIEW,
    COMPLETED,
    REJECTED
}
