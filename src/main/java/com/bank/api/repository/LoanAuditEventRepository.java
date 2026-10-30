package com.bank.api.repository;

import com.bank.api.model.LoanAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanAuditEventRepository extends JpaRepository<LoanAuditEvent, Long> {
}
