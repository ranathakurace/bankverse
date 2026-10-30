package com.bank.api.service;

import com.bank.api.dto.PaymentActionRequest;
import com.bank.api.dto.PaymentInitiationRequest;
import com.bank.api.dto.PaymentResponse;
import com.bank.api.exception.AccountNotFoundException;
import com.bank.api.exception.InvalidPaymentException;
import com.bank.api.exception.PaymentWorkflowException;
import com.bank.api.model.Account;
import com.bank.api.model.AccountStatus;
import com.bank.api.model.LedgerTransaction;
import com.bank.api.model.LedgerTransactionType;
import com.bank.api.model.Payment;
import com.bank.api.model.PaymentAuditEvent;
import com.bank.api.model.PaymentStatus;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.LedgerTransactionRepository;
import com.bank.api.repository.PaymentAuditEventRepository;
import com.bank.api.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private static final String ACCOUNT_NUMBER_REGEX = "^ACC\\d{6}$";
    private final PaymentRepository paymentRepository;
    private final PaymentAuditEventRepository paymentAuditEventRepository;
    private final AccountRepository accountRepository;
    private final LedgerTransactionRepository ledgerTransactionRepository;
    public PaymentService(PaymentRepository paymentRepository, PaymentAuditEventRepository paymentAuditEventRepository,
            AccountRepository accountRepository, LedgerTransactionRepository ledgerTransactionRepository) {
        this.paymentRepository = paymentRepository; this.paymentAuditEventRepository = paymentAuditEventRepository;
        this.accountRepository = accountRepository; this.ledgerTransactionRepository = ledgerTransactionRepository;
    }

    @Transactional
    public PaymentResponse initiate(String idempotencyKey, PaymentInitiationRequest request) {
        validateKey(idempotencyKey);
        Optional<Payment> previous = paymentRepository.findByIdempotencyKey(idempotencyKey.trim());
        if (previous.isPresent()) { return toResponse(previous.get()); }
        validateInitiation(request);
        Account account = findAccount(request.getSourceAccountNumber());
        if (account.getAccountStatus() != AccountStatus.ACTIVE) { throw new PaymentWorkflowException("Payment initiation requires an ACTIVE source account."); }
        Payment payment = new Payment(); payment.setPaymentReference("PAY-" + UUID.randomUUID()); payment.setSourceAccount(account);
        payment.setMethod(request.getMethod()); payment.setStatus(PaymentStatus.INITIATED); payment.setAmount(request.getAmount());
        payment.setCurrency(account.getCurrency()); payment.setBeneficiaryReference(request.getBeneficiaryReference().trim()); payment.setIdempotencyKey(idempotencyKey.trim()); payment.setAttemptCount(1); payment.setInitiatedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment); recordAudit(saved, "PAYMENT_INITIATED", null, PaymentStatus.INITIATED, "SYSTEM", "Payment initiated.");
        return toResponse(saved);
    }

    @Transactional
    public PaymentResponse authorize(String paymentReference, PaymentActionRequest request) {
        Payment payment = findPayment(paymentReference); validateAction(request, false);
        changeStatus(payment, PaymentStatus.INITIATED, PaymentStatus.AUTHORIZED, "PAYMENT_AUTHORIZED", request, null);
        payment.setAuthorizedAt(LocalDateTime.now()); paymentRepository.save(payment); return toResponse(payment);
    }

    @Transactional
    public PaymentResponse execute(String paymentReference, PaymentActionRequest request) {
        Payment payment = findPayment(paymentReference); validateAction(request, false);
        if (payment.getStatus() != PaymentStatus.AUTHORIZED) { throw new PaymentWorkflowException("Only AUTHORIZED payments can be executed."); }
        Account account = accountRepository.findByAccountNumberForUpdate(payment.getSourceAccount().getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(payment.getSourceAccount().getAccountNumber()));
        if (account.getAccountStatus() != AccountStatus.ACTIVE) { throw new PaymentWorkflowException("Payment execution requires an ACTIVE source account."); }
        if (account.getBalance() == null || account.getBalance().compareTo(payment.getAmount()) < 0) { throw new PaymentWorkflowException("Insufficient account balance for payment."); }
        BigDecimal balanceAfter = account.getBalance().subtract(payment.getAmount()); account.setBalance(balanceAfter); accountRepository.save(account);
        LedgerTransaction entry = new LedgerTransaction(); entry.setReference("TXN-" + UUID.randomUUID()); entry.setAccount(account); entry.setType(LedgerTransactionType.PAYMENT_DEBIT); entry.setAmount(payment.getAmount()); entry.setBalanceAfter(balanceAfter); entry.setCurrency(account.getCurrency()); entry.setCounterpartyAccountNumber(payment.getBeneficiaryReference()); entry.setDescription("Payment " + payment.getPaymentReference()); entry.setOccurredAt(LocalDateTime.now()); ledgerTransactionRepository.save(entry);
        changeStatus(payment, PaymentStatus.AUTHORIZED, PaymentStatus.COMPLETED, "PAYMENT_COMPLETED", request, null); payment.setCompletedAt(LocalDateTime.now()); paymentRepository.save(payment); return toResponse(payment);
    }

    @Transactional
    public PaymentResponse fail(String paymentReference, PaymentActionRequest request) {
        Payment payment = findPayment(paymentReference); validateAction(request, true);
        if (payment.getStatus() != PaymentStatus.INITIATED && payment.getStatus() != PaymentStatus.AUTHORIZED) { throw new PaymentWorkflowException("Only INITIATED or AUTHORIZED payments can be failed."); }
        PaymentStatus previous = payment.getStatus(); payment.setStatus(PaymentStatus.FAILED); payment.setFailureReason(request.getRemarks().trim()); paymentRepository.save(payment); recordAudit(payment, "PAYMENT_FAILED", previous, PaymentStatus.FAILED, request.getPerformedBy().trim(), request.getRemarks().trim()); return toResponse(payment);
    }

    @Transactional
    public PaymentResponse retry(String paymentReference, PaymentActionRequest request) {
        Payment payment = findPayment(paymentReference); validateAction(request, false);
        if (payment.getStatus() != PaymentStatus.FAILED) { throw new PaymentWorkflowException("Only FAILED payments can be retried."); }
        payment.setStatus(PaymentStatus.INITIATED); payment.setFailureReason(null); payment.setAttemptCount(payment.getAttemptCount() + 1); paymentRepository.save(payment); recordAudit(payment, "PAYMENT_RETRIED", PaymentStatus.FAILED, PaymentStatus.INITIATED, request.getPerformedBy().trim(), trimOptional(request.getRemarks())); return toResponse(payment);
    }

    @Transactional
    public PaymentResponse refund(String paymentReference, PaymentActionRequest request) {
        Payment payment = findPayment(paymentReference); validateAction(request, true);
        if (payment.getStatus() != PaymentStatus.COMPLETED) { throw new PaymentWorkflowException("Only COMPLETED payments can be refunded."); }
        Account account = accountRepository.findByAccountNumberForUpdate(payment.getSourceAccount().getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(payment.getSourceAccount().getAccountNumber()));
        BigDecimal balanceAfter = account.getBalance().add(payment.getAmount()); account.setBalance(balanceAfter); accountRepository.save(account);
        LedgerTransaction entry = new LedgerTransaction(); entry.setReference("TXN-" + UUID.randomUUID()); entry.setAccount(account); entry.setType(LedgerTransactionType.PAYMENT_REFUND); entry.setAmount(payment.getAmount()); entry.setBalanceAfter(balanceAfter); entry.setCurrency(account.getCurrency()); entry.setCounterpartyAccountNumber(payment.getBeneficiaryReference()); entry.setDescription("Refund " + payment.getPaymentReference()); entry.setOccurredAt(LocalDateTime.now()); ledgerTransactionRepository.save(entry);
        changeStatus(payment, PaymentStatus.COMPLETED, PaymentStatus.REFUNDED, "PAYMENT_REFUNDED", request, null); payment.setRefundedAt(LocalDateTime.now()); paymentRepository.save(payment); return toResponse(payment);
    }

    @Transactional(readOnly = true) public PaymentResponse get(String paymentReference) { return toResponse(findPayment(paymentReference)); }
    private void changeStatus(Payment payment, PaymentStatus expected, PaymentStatus target, String action, PaymentActionRequest request, String failure) { if (payment.getStatus() != expected) { throw new PaymentWorkflowException("Payment must be in " + expected + " status for this operation."); } payment.setStatus(target); payment.setFailureReason(failure); paymentRepository.save(payment); recordAudit(payment, action, expected, target, request.getPerformedBy().trim(), trimOptional(request.getRemarks())); }
    private Payment findPayment(String ref) { if (isBlank(ref)) { throw new InvalidPaymentException("Payment reference is mandatory."); } return paymentRepository.findByPaymentReference(ref.trim()).orElseThrow(() -> new PaymentWorkflowException("Payment was not found.")); }
    private Account findAccount(String no) { String accountNumber = normalizeAccountNumber(no); return accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountNotFoundException(accountNumber)); }
    private void validateInitiation(PaymentInitiationRequest request) { if (request == null || request.getMethod() == null || isBlank(request.getBeneficiaryReference())) { throw new InvalidPaymentException("Source account number, payment method, and beneficiary reference are mandatory."); } normalizeAccountNumber(request.getSourceAccountNumber()); if (request.getAmount() == null || request.getAmount().signum() <= 0 || request.getAmount().scale() > 2) { throw new InvalidPaymentException("Amount must be greater than zero with at most two decimal places."); } if (request.getBeneficiaryReference().trim().length() > 100) { throw new InvalidPaymentException("Beneficiary reference cannot exceed 100 characters."); } }
    private void validateAction(PaymentActionRequest request, boolean remarksRequired) { if (request == null || isBlank(request.getPerformedBy())) { throw new InvalidPaymentException("Performed by is mandatory."); } if (request.getPerformedBy().trim().length() > 100) { throw new InvalidPaymentException("Performed by cannot exceed 100 characters."); } if (remarksRequired && isBlank(request.getRemarks())) { throw new InvalidPaymentException("Remarks are mandatory for this action."); } if (request.getRemarks() != null && request.getRemarks().trim().length() > 500) { throw new InvalidPaymentException("Remarks cannot exceed 500 characters."); } }
    private void validateKey(String key) { if (isBlank(key) || key.trim().length() > 100) { throw new InvalidPaymentException("Idempotency-Key must contain between 1 and 100 characters."); } }
    private String normalizeAccountNumber(String value) { if (isBlank(value)) { throw new InvalidPaymentException("Source account number is mandatory."); } String normalized = value.trim().toUpperCase(Locale.ROOT); if (!normalized.matches(ACCOUNT_NUMBER_REGEX)) { throw new InvalidPaymentException("Invalid account number format."); } return normalized; }
    private void recordAudit(Payment payment, String action, PaymentStatus previous, PaymentStatus next, String by, String remarks) { PaymentAuditEvent event = new PaymentAuditEvent(); event.setPayment(payment); event.setAction(action); event.setPreviousStatus(previous); event.setNewStatus(next); event.setPerformedBy(by); event.setRemarks(remarks); event.setOccurredAt(LocalDateTime.now()); paymentAuditEventRepository.save(event); }
    private PaymentResponse toResponse(Payment p) { PaymentResponse r = new PaymentResponse(); r.setPaymentReference(p.getPaymentReference()); r.setSourceAccountNumber(p.getSourceAccount().getAccountNumber()); r.setMethod(p.getMethod().name()); r.setStatus(p.getStatus().name()); r.setAmount(p.getAmount()); r.setCurrency(p.getCurrency()); r.setBeneficiaryReference(p.getBeneficiaryReference()); r.setAttemptCount(p.getAttemptCount()); return r; }
    private String trimOptional(String v) { return v == null ? null : v.trim(); } private boolean isBlank(String v) { return v == null || v.trim().isEmpty(); }
}
