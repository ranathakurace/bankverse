package com.bank.api.repository;

import com.bank.api.model.AccountAuditEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountAuditEventRepository extends JpaRepository<AccountAuditEvent, Long> {

    List<AccountAuditEvent> findByAccount_IdOrderByOccurredAtAsc(Long accountId);
}
