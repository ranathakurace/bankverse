package com.bank.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.api.model.KycAuditEvent;

public interface KycAuditEventRepository extends JpaRepository<KycAuditEvent, Long> {

    List<KycAuditEvent> findByCustomer_IdOrderByOccurredAtAsc(Long customerId);
}
