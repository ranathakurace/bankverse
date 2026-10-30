package com.bank.api.model;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
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
