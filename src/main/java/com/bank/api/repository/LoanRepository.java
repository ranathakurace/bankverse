package com.bank.api.repository;

import com.bank.api.model.Loan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findByLoanReference(String loanReference);
    Optional<Loan> findByDisbursementIdempotencyKey(String disbursementIdempotencyKey);
}
