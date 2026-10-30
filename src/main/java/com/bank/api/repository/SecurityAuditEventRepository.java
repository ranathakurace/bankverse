package com.bank.api.repository;
import com.bank.api.model.SecurityAuditEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface SecurityAuditEventRepository extends JpaRepository<SecurityAuditEvent, Long>, JpaSpecificationExecutor<SecurityAuditEvent> { List<SecurityAuditEvent> findTop100ByOrderByOccurredAtDesc(); }
