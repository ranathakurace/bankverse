package com.bank.api.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "loan_reference", nullable = false, unique = true, length = 50)
    private String loanReference;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disbursement_account_id")
    private Account disbursementAccount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principal;
    @Column(name = "annual_interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualInterestRate;
    @Column(name = "term_months", nullable = false)
    private Integer termMonths;
    @Column(name = "remaining_principal", nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingPrincipal;
    @Column(name = "disbursement_idempotency_key", unique = true, length = 100)
    private String disbursementIdempotencyKey;
    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;
}
