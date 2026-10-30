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

@Data @NoArgsConstructor @Entity @Table(name = "risk_assessments")
public class RiskAssessment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "assessment_reference", nullable = false, unique = true, length = 50) private String assessmentReference;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id", nullable = false) private Account account;
    @Column(name = "operation_type", nullable = false, length = 50) private String operationType;
    @Column(name = "operation_reference", length = 100) private String operationReference;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(name = "risk_score", nullable = false) private Integer riskScore;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RiskDecision decision;
    @Column(nullable = false, length = 500) private String reason;
    @Column(name = "assessed_at", nullable = false) private LocalDateTime assessedAt;
}
