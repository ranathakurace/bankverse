package com.bank.api.service;

import com.bank.api.dto.LoanApplicationRequest;
import com.bank.api.dto.LoanDecisionRequest;
import com.bank.api.dto.LoanDisbursementRequest;
import com.bank.api.dto.LoanResponse;
import com.bank.api.exception.AccountNotFoundException;
import com.bank.api.exception.CustomerNotFoundException;
import com.bank.api.exception.InvalidLoanException;
import com.bank.api.exception.LoanWorkflowException;
import com.bank.api.model.Account;
import com.bank.api.model.AccountStatus;
import com.bank.api.model.Customer;
import com.bank.api.model.CustomerStatus;
import com.bank.api.model.LedgerTransaction;
import com.bank.api.model.LedgerTransactionType;
import com.bank.api.model.Loan;
import com.bank.api.model.LoanAuditEvent;
import com.bank.api.model.LoanStatus;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.CustomerRepository;
import com.bank.api.repository.LedgerTransactionRepository;
import com.bank.api.repository.LoanAuditEventRepository;
import com.bank.api.repository.LoanRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {
    private static final String CUSTOMER_NUMBER_REGEX = "^CUST\\d{6}$";
    private static final String ACCOUNT_NUMBER_REGEX = "^ACC\\d{6}$";
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;
    private final LoanAuditEventRepository loanAuditEventRepository;
    private final LedgerTransactionRepository ledgerTransactionRepository;

    public LoanService(CustomerRepository customerRepository, AccountRepository accountRepository,
            LoanRepository loanRepository, LoanAuditEventRepository loanAuditEventRepository,
            LedgerTransactionRepository ledgerTransactionRepository) {
        this.customerRepository = customerRepository; this.accountRepository = accountRepository;
        this.loanRepository = loanRepository; this.loanAuditEventRepository = loanAuditEventRepository;
        this.ledgerTransactionRepository = ledgerTransactionRepository;
    }

    @Transactional
    public LoanResponse apply(LoanApplicationRequest request) {
        validateApplication(request);
        Customer customer = findCustomer(request.getCustomerNumber());
        if (customer.getCustomerStatus() != CustomerStatus.ACTIVE) {
            throw new LoanWorkflowException("Only an ACTIVE customer can apply for a loan.");
        }
        LocalDateTime now = LocalDateTime.now();
        Loan loan = new Loan(); loan.setLoanReference("LOAN-" + UUID.randomUUID()); loan.setCustomer(customer);
        loan.setType(request.getLoanType()); loan.setStatus(LoanStatus.APPLIED); loan.setPrincipal(request.getPrincipal());
        loan.setAnnualInterestRate(request.getAnnualInterestRate()); loan.setTermMonths(request.getTermMonths());
        loan.setRemainingPrincipal(request.getPrincipal()); loan.setAppliedAt(now);
        Loan savedLoan = loanRepository.save(loan);
        recordAudit(savedLoan, "LOAN_APPLIED", null, LoanStatus.APPLIED, "CUSTOMER", "Loan application submitted.", now);
        return toResponse(savedLoan);
    }

    @Transactional
    public LoanResponse approve(String loanReference, LoanDecisionRequest request) {
        Loan loan = findLoan(loanReference); validateDecision(request, false);
        changeDecision(loan, request, LoanStatus.APPROVED, "LOAN_APPROVED"); return toResponse(loan);
    }

    @Transactional
    public LoanResponse reject(String loanReference, LoanDecisionRequest request) {
        Loan loan = findLoan(loanReference); validateDecision(request, true);
        changeDecision(loan, request, LoanStatus.REJECTED, "LOAN_REJECTED"); return toResponse(loan);
    }

    @Transactional
    public LoanResponse disburse(String loanReference, String idempotencyKey, LoanDisbursementRequest request) {
        validateDisbursement(request, idempotencyKey);
        Optional<Loan> prior = loanRepository.findByDisbursementIdempotencyKey(idempotencyKey.trim());
        if (prior.isPresent()) {
            if (!prior.get().getLoanReference().equals(loanReference)) { throw new LoanWorkflowException("Idempotency key has already been used for another loan."); }
            return toResponse(prior.get());
        }
        Loan loan = findLoan(loanReference);
        if (loan.getStatus() != LoanStatus.APPROVED) { throw new LoanWorkflowException("Only APPROVED loans can be disbursed."); }
        Account account = findLockedAccount(request.getAccountNumber());
        if (account.getAccountStatus() != AccountStatus.ACTIVE) { throw new LoanWorkflowException("Loan can only be disbursed to an ACTIVE account."); }
        if (!account.getCustomer().getId().equals(loan.getCustomer().getId())) { throw new LoanWorkflowException("Disbursement account does not belong to the loan customer."); }
        BigDecimal balanceAfter = account.getBalance().add(loan.getPrincipal());
        account.setBalance(balanceAfter); accountRepository.save(account);
        LedgerTransaction transaction = new LedgerTransaction(); transaction.setReference("TXN-" + UUID.randomUUID());
        transaction.setAccount(account); transaction.setType(LedgerTransactionType.LOAN_DISBURSEMENT);
        transaction.setAmount(loan.getPrincipal()); transaction.setBalanceAfter(balanceAfter); transaction.setCurrency(account.getCurrency());
        transaction.setIdempotencyKey(idempotencyKey.trim()); transaction.setDescription(request.getReason().trim()); transaction.setOccurredAt(LocalDateTime.now());
        ledgerTransactionRepository.save(transaction);
        LoanStatus previous = loan.getStatus(); loan.setStatus(LoanStatus.DISBURSED); loan.setDisbursementAccount(account);
        loan.setDisbursementIdempotencyKey(idempotencyKey.trim()); loan.setDisbursedAt(LocalDateTime.now()); loanRepository.save(loan);
        recordAudit(loan, "LOAN_DISBURSED", previous, LoanStatus.DISBURSED, request.getPerformedBy().trim(), request.getReason().trim(), LocalDateTime.now());
        return toResponse(loan);
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoan(String loanReference) { return toResponse(findLoan(loanReference)); }

    private void changeDecision(Loan loan, LoanDecisionRequest request, LoanStatus outcome, String action) {
        if (loan.getStatus() != LoanStatus.APPLIED) { throw new LoanWorkflowException("Only APPLIED loans can be decided."); }
        loan.setStatus(outcome); loan.setApprovedAt(outcome == LoanStatus.APPROVED ? LocalDateTime.now() : null); loanRepository.save(loan);
        recordAudit(loan, action, LoanStatus.APPLIED, outcome, request.getPerformedBy().trim(), trimOptional(request.getReason()), LocalDateTime.now());
    }
    private Customer findCustomer(String number) {
        String normalized = normalize(number, CUSTOMER_NUMBER_REGEX, "customer number");
        return customerRepository.findByCustomerNumber(normalized).orElseThrow(() -> new CustomerNotFoundException("customer number", normalized));
    }
    private Account findLockedAccount(String number) {
        String normalized = normalize(number, ACCOUNT_NUMBER_REGEX, "account number");
        return accountRepository.findByAccountNumberForUpdate(normalized).orElseThrow(() -> new AccountNotFoundException(normalized));
    }
    private Loan findLoan(String reference) {
        if (isBlank(reference)) { throw new InvalidLoanException("Loan reference is mandatory."); }
        return loanRepository.findByLoanReference(reference.trim()).orElseThrow(() -> new LoanWorkflowException("Loan was not found."));
    }
    private void validateApplication(LoanApplicationRequest request) {
        if (request == null || request.getLoanType() == null) { throw new InvalidLoanException("Customer number and loan type are mandatory."); }
        normalize(request.getCustomerNumber(), CUSTOMER_NUMBER_REGEX, "customer number"); validateMoney(request.getPrincipal(), "Principal", new BigDecimal("0.01"));
        validateMoney(request.getAnnualInterestRate(), "Annual interest rate", new BigDecimal("0.01"));
        if (request.getAnnualInterestRate().compareTo(new BigDecimal("100.00")) > 0) { throw new InvalidLoanException("Annual interest rate cannot exceed 100.00."); }
        if (request.getTermMonths() == null || request.getTermMonths() < 1 || request.getTermMonths() > 480) { throw new InvalidLoanException("Term months must be between 1 and 480."); }
    }
    private void validateDecision(LoanDecisionRequest request, boolean reasonMandatory) {
        if (request == null || isBlank(request.getPerformedBy())) { throw new InvalidLoanException("Performed by is mandatory."); }
        if (request.getPerformedBy().trim().length() > 100) { throw new InvalidLoanException("Performed by cannot exceed 100 characters."); }
        if (reasonMandatory && isBlank(request.getReason())) { throw new InvalidLoanException("Reason is mandatory when rejecting a loan."); }
        if (request.getReason() != null && request.getReason().trim().length() > 500) { throw new InvalidLoanException("Reason cannot exceed 500 characters."); }
    }
    private void validateDisbursement(LoanDisbursementRequest request, String key) {
        if (isBlank(key) || key.trim().length() > 100) { throw new InvalidLoanException("Idempotency-Key must contain between 1 and 100 characters."); }
        if (request == null || isBlank(request.getPerformedBy()) || isBlank(request.getReason())) { throw new InvalidLoanException("Account number, performed by, and reason are mandatory."); }
        normalize(request.getAccountNumber(), ACCOUNT_NUMBER_REGEX, "account number");
        if (request.getPerformedBy().trim().length() > 100 || request.getReason().trim().length() > 500) { throw new InvalidLoanException("Performed by or reason exceeds its maximum length."); }
    }
    private void validateMoney(BigDecimal value, String label, BigDecimal minimum) {
        if (value == null || value.compareTo(minimum) < 0 || value.scale() > 2) { throw new InvalidLoanException(label + " must be positive with at most two decimal places."); }
    }
    private void recordAudit(Loan loan, String action, LoanStatus previous, LoanStatus current, String by, String reason, LocalDateTime now) {
        LoanAuditEvent event = new LoanAuditEvent(); event.setLoan(loan); event.setAction(action); event.setPreviousStatus(previous); event.setNewStatus(current); event.setPerformedBy(by); event.setReason(reason); event.setOccurredAt(now); loanAuditEventRepository.save(event);
    }
    private LoanResponse toResponse(Loan loan) {
        LoanResponse response = new LoanResponse(); response.setLoanReference(loan.getLoanReference()); response.setCustomerNumber(loan.getCustomer().getCustomerNumber());
        response.setDisbursementAccountNumber(loan.getDisbursementAccount() == null ? null : loan.getDisbursementAccount().getAccountNumber()); response.setLoanType(loan.getType().name()); response.setStatus(loan.getStatus().name()); response.setPrincipal(loan.getPrincipal()); response.setAnnualInterestRate(loan.getAnnualInterestRate()); response.setTermMonths(loan.getTermMonths()); response.setRemainingPrincipal(loan.getRemainingPrincipal()); return response;
    }
    private String normalize(String value, String regex, String label) { if (isBlank(value)) { throw new InvalidLoanException(label + " is mandatory."); } String normalized = value.trim().toUpperCase(Locale.ROOT); if (!normalized.matches(regex)) { throw new InvalidLoanException("Invalid " + label + " format."); } return normalized; }
    private String trimOptional(String value) { return value == null ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
