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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @Entity @Table(name = "payment_audit_events")
public class PaymentAuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "payment_id", nullable = false) private Payment payment;
    @Column(nullable = false, length = 50) private String action;
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", length = 20) private PaymentStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false, length = 20) private PaymentStatus newStatus;
    @Column(name = "performed_by", nullable = false, length = 100) private String performedBy;
    @Column(length = 500) private String remarks;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
}
