package com.bank.api.dto;
import java.time.LocalDateTime;
import lombok.Data;
@Data public class SecurityAuditResponse { private String actorUsername; private String action; private String targetUsername; private String details; private LocalDateTime occurredAt; }
