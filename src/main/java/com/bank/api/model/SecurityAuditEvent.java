package com.bank.api.model;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @Entity @Table(name = "security_audit_events")
public class SecurityAuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "actor_username", nullable = false, length = 100) private String actorUsername;
    @Column(nullable = false, length = 100) private String action;
    @Column(name = "target_username", length = 100) private String targetUsername;
    @Column(length = 500) private String details;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
}
