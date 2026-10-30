package com.bank.api.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "payment_reference", nullable = false, unique = true, length = 50) private String paymentReference;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "source_account_id", nullable = false) private Account sourceAccount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private PaymentMethod method;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatus status;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "beneficiary_reference", nullable = false, length = 100) private String beneficiaryReference;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100) private String idempotencyKey;
    @Column(name = "failure_reason", length = 500) private String failureReason;
    @Column(name = "attempt_count", nullable = false) private Integer attemptCount;
    @Column(name = "initiated_at", nullable = false) private LocalDateTime initiatedAt;
    @Column(name = "authorized_at") private LocalDateTime authorizedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "refunded_at") private LocalDateTime refundedAt;
}
