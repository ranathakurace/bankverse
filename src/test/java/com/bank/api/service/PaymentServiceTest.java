package com.bank.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

import com.bank.api.dto.PaymentActionRequest;
import com.bank.api.dto.PaymentInitiationRequest;
import com.bank.api.dto.PaymentResponse;
import com.bank.api.exception.PaymentWorkflowException;
import com.bank.api.model.Account;
import com.bank.api.model.AccountStatus;
import com.bank.api.model.LedgerTransaction;
import com.bank.api.model.Payment;
import com.bank.api.model.PaymentAuditEvent;
import com.bank.api.model.PaymentMethod;
import com.bank.api.model.PaymentStatus;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.LedgerTransactionRepository;
import com.bank.api.repository.PaymentAuditEventRepository;
import com.bank.api.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PaymentServiceTest {
    private PaymentRepository paymentRepository; private PaymentAuditEventRepository paymentAuditEventRepository; private AccountRepository accountRepository; private LedgerTransactionRepository ledgerTransactionRepository; private PaymentService paymentService;
    @BeforeMethod public void setUp() { paymentRepository = mock(PaymentRepository.class); paymentAuditEventRepository = mock(PaymentAuditEventRepository.class); accountRepository = mock(AccountRepository.class); ledgerTransactionRepository = mock(LedgerTransactionRepository.class); paymentService = new PaymentService(paymentRepository, paymentAuditEventRepository, accountRepository, ledgerTransactionRepository); when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0)); when(ledgerTransactionRepository.save(any(LedgerTransaction.class))).thenAnswer(i -> i.getArgument(0)); }
    @Test public void initiatesIdempotentPaymentForActiveAccount() {
        Account account = account("100.00"); when(accountRepository.findByAccountNumber("ACC000101")).thenReturn(Optional.of(account));
        PaymentResponse response = paymentService.initiate("payment-1", initiation());
        assertEquals(response.getStatus(), PaymentStatus.INITIATED.name()); assertEquals(response.getMethod(), PaymentMethod.UPI.name()); verify(paymentAuditEventRepository).save(any(PaymentAuditEvent.class));
    }
    @Test public void returnsExistingPaymentForSameInitiationKey() {
        Payment payment = payment(PaymentStatus.INITIATED, "100.00"); when(paymentRepository.findByIdempotencyKey("payment-1")).thenReturn(Optional.of(payment));
        assertEquals(paymentService.initiate("payment-1", initiation()).getPaymentReference(), "PAY-1"); verify(accountRepository, never()).findByAccountNumber(any());
    }
    @Test public void authorizesInitiatedPayment() {
        Payment payment = payment(PaymentStatus.INITIATED, "100.00"); when(paymentRepository.findByPaymentReference("PAY-1")).thenReturn(Optional.of(payment));
        assertEquals(paymentService.authorize("PAY-1", action("Authorized.")).getStatus(), PaymentStatus.AUTHORIZED.name());
    }
    @Test public void executesAuthorizedPaymentAndWritesDebitLedgerEntry() {
        Payment payment = payment(PaymentStatus.AUTHORIZED, "100.00"); Account account = account("100.00"); when(paymentRepository.findByPaymentReference("PAY-1")).thenReturn(Optional.of(payment)); when(accountRepository.findByAccountNumberForUpdate("ACC000101")).thenReturn(Optional.of(account));
        PaymentResponse response = paymentService.execute("PAY-1", action("Rail accepted."));
        assertEquals(response.getStatus(), PaymentStatus.COMPLETED.name()); assertEquals(account.getBalance(), new BigDecimal("0.00")); verify(ledgerTransactionRepository).save(any(LedgerTransaction.class));
    }
    @Test public void preventsExecutionWhenBalanceIsInsufficient() {
        Payment payment = payment(PaymentStatus.AUTHORIZED, "101.00"); when(paymentRepository.findByPaymentReference("PAY-1")).thenReturn(Optional.of(payment)); when(accountRepository.findByAccountNumberForUpdate("ACC000101")).thenReturn(Optional.of(account("100.00")));
        expectThrows(PaymentWorkflowException.class, () -> paymentService.execute("PAY-1", action("Execute.")));
    }
    @Test public void retriesFailedPayment() {
        Payment payment = payment(PaymentStatus.FAILED, "10.00"); payment.setAttemptCount(1); when(paymentRepository.findByPaymentReference("PAY-1")).thenReturn(Optional.of(payment));
        PaymentResponse response = paymentService.retry("PAY-1", action("Retry requested."));
        assertEquals(response.getStatus(), PaymentStatus.INITIATED.name()); assertEquals(response.getAttemptCount(), Integer.valueOf(2));
    }
    @Test public void refundsCompletedPaymentAndCreditsAccount() {
        Payment payment = payment(PaymentStatus.COMPLETED, "10.00"); Account account = account("90.00"); when(paymentRepository.findByPaymentReference("PAY-1")).thenReturn(Optional.of(payment)); when(accountRepository.findByAccountNumberForUpdate("ACC000101")).thenReturn(Optional.of(account));
        PaymentResponse response = paymentService.refund("PAY-1", action("Merchant refund approved."));
        assertEquals(response.getStatus(), PaymentStatus.REFUNDED.name()); assertEquals(account.getBalance(), new BigDecimal("100.00")); verify(ledgerTransactionRepository).save(any(LedgerTransaction.class));
    }
    private Account account(String balance) { Account account = new Account(); account.setId(101L); account.setAccountNumber("ACC000101"); account.setAccountStatus(AccountStatus.ACTIVE); account.setBalance(new BigDecimal(balance)); account.setCurrency("INR"); return account; }
    private Payment payment(PaymentStatus status, String amount) { Payment payment = new Payment(); payment.setPaymentReference("PAY-1"); payment.setSourceAccount(account("100.00")); payment.setMethod(PaymentMethod.UPI); payment.setStatus(status); payment.setAmount(new BigDecimal(amount)); payment.setCurrency("INR"); payment.setBeneficiaryReference("merchant@upi"); payment.setAttemptCount(1); return payment; }
    private PaymentInitiationRequest initiation() { PaymentInitiationRequest request = new PaymentInitiationRequest(); request.setSourceAccountNumber("ACC000101"); request.setMethod(PaymentMethod.UPI); request.setAmount(new BigDecimal("10.00")); request.setBeneficiaryReference("merchant@upi"); return request; }
    private PaymentActionRequest action(String remarks) { PaymentActionRequest request = new PaymentActionRequest(); request.setPerformedBy("OPS-6001"); request.setRemarks(remarks); return request; }
}
