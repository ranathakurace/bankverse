package com.bank.api.repository;
import com.bank.api.model.PaymentAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentAuditEventRepository extends JpaRepository<PaymentAuditEvent, Long> { }
