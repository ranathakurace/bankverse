package com.bank.api.repository;

import com.bank.api.model.CardAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardAuditEventRepository extends JpaRepository<CardAuditEvent, Long> {
}
