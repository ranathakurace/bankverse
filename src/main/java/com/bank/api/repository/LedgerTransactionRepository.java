package com.bank.api.repository;

import com.bank.api.model.LedgerTransaction;
import com.bank.api.model.LedgerTransactionType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, Long> {

    Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<LedgerTransaction> findByTransferReferenceAndType(
            String transferReference, LedgerTransactionType type);

    Page<LedgerTransaction> findByAccount_IdOrderByOccurredAtDesc(Long accountId, Pageable pageable);

    Page<LedgerTransaction> findByAccount_IdAndTypeOrderByOccurredAtDesc(
            Long accountId, LedgerTransactionType type, Pageable pageable);
}
