package com.bank.api.model;

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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @Entity @Table(name = "control_cases")
public class ControlCase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "case_reference", nullable = false, unique = true, length = 50) private String caseReference;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "assessment_id", nullable = false, unique = true) private RiskAssessment assessment;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ControlCaseStatus status;
    @Column(name = "opened_at", nullable = false) private LocalDateTime openedAt;
    @Column(name = "resolved_at") private LocalDateTime resolvedAt;
    @Column(name = "resolved_by", length = 100) private String resolvedBy;
    @Column(name = "resolution_notes", length = 500) private String resolutionNotes;
}
