package com.bank.api.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
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
