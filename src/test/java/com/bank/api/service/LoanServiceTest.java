package com.bank.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

import com.bank.api.dto.LoanApplicationRequest;
import com.bank.api.dto.LoanDecisionRequest;
import com.bank.api.dto.LoanDisbursementRequest;
import com.bank.api.dto.LoanResponse;
import com.bank.api.exception.LoanWorkflowException;
import com.bank.api.model.Account;
import com.bank.api.model.AccountStatus;
import com.bank.api.model.Customer;
import com.bank.api.model.CustomerStatus;
import com.bank.api.model.LedgerTransaction;
import com.bank.api.model.Loan;
import com.bank.api.model.LoanAuditEvent;
import com.bank.api.model.LoanStatus;
import com.bank.api.model.LoanType;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.CustomerRepository;
import com.bank.api.repository.LedgerTransactionRepository;
import com.bank.api.repository.LoanAuditEventRepository;
import com.bank.api.repository.LoanRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LoanServiceTest {
    private CustomerRepository customerRepository; private AccountRepository accountRepository; private LoanRepository loanRepository; private LoanAuditEventRepository loanAuditEventRepository; private LedgerTransactionRepository ledgerTransactionRepository; private LoanService loanService;
    @BeforeMethod public void setUp() { customerRepository = mock(CustomerRepository.class); accountRepository = mock(AccountRepository.class); loanRepository = mock(LoanRepository.class); loanAuditEventRepository = mock(LoanAuditEventRepository.class); ledgerTransactionRepository = mock(LedgerTransactionRepository.class); loanService = new LoanService(customerRepository, accountRepository, loanRepository, loanAuditEventRepository, ledgerTransactionRepository); when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArgument(0)); when(ledgerTransactionRepository.save(any(LedgerTransaction.class))).thenAnswer(i -> i.getArgument(0)); }
    @Test public void createsLoanApplicationForActiveCustomer() {
        when(customerRepository.findByCustomerNumber("CUST000003")).thenReturn(Optional.of(customer()));
        LoanResponse response = loanService.apply(application());
        assertEquals(response.getStatus(), LoanStatus.APPLIED.name()); assertEquals(response.getRemainingPrincipal(), new BigDecimal("1000.00")); verify(loanAuditEventRepository).save(any(LoanAuditEvent.class));
    }
    @Test public void approvesOnlyAppliedLoan() {
        Loan loan = loan(LoanStatus.APPLIED); when(loanRepository.findByLoanReference("LOAN-1")).thenReturn(Optional.of(loan));
        assertEquals(loanService.approve("LOAN-1", decision("Approved by underwriter.")).getStatus(), LoanStatus.APPROVED.name());
    }
    @Test public void rejectsDecisionForAlreadyApprovedLoan() {
        Loan loan = loan(LoanStatus.APPROVED); when(loanRepository.findByLoanReference("LOAN-1")).thenReturn(Optional.of(loan));
        expectThrows(LoanWorkflowException.class, () -> loanService.reject("LOAN-1", decision("Duplicate decision.")));
    }
    @Test public void disbursesApprovedLoanToOwnedActiveAccountAndWritesLedger() {
        Loan loan = loan(LoanStatus.APPROVED); Account account = account(customer()); when(loanRepository.findByLoanReference("LOAN-1")).thenReturn(Optional.of(loan)); when(accountRepository.findByAccountNumberForUpdate("ACC000101")).thenReturn(Optional.of(account));
        LoanResponse response = loanService.disburse("LOAN-1", "loan-disburse-1", disbursement());
        assertEquals(response.getStatus(), LoanStatus.DISBURSED.name()); assertEquals(account.getBalance(), new BigDecimal("1100.00")); verify(ledgerTransactionRepository).save(any(LedgerTransaction.class));
    }
    @Test public void preventsDisbursementToDifferentCustomerAccount() {
        Loan loan = loan(LoanStatus.APPROVED); Customer different = customer(); different.setId(4L); when(loanRepository.findByLoanReference("LOAN-1")).thenReturn(Optional.of(loan)); when(accountRepository.findByAccountNumberForUpdate("ACC000101")).thenReturn(Optional.of(account(different)));
        expectThrows(LoanWorkflowException.class, () -> loanService.disburse("LOAN-1", "loan-disburse-1", disbursement()));
    }
    private Customer customer() { Customer customer = new Customer(); customer.setId(3L); customer.setCustomerNumber("CUST000003"); customer.setCustomerStatus(CustomerStatus.ACTIVE); return customer; }
    private Account account(Customer customer) { Account account = new Account(); account.setId(101L); account.setAccountNumber("ACC000101"); account.setCustomer(customer); account.setAccountStatus(AccountStatus.ACTIVE); account.setBalance(new BigDecimal("100.00")); account.setCurrency("INR"); return account; }
    private Loan loan(LoanStatus status) { Loan loan = new Loan(); loan.setLoanReference("LOAN-1"); loan.setCustomer(customer()); loan.setType(LoanType.PERSONAL); loan.setStatus(status); loan.setPrincipal(new BigDecimal("1000.00")); loan.setAnnualInterestRate(new BigDecimal("12.50")); loan.setTermMonths(12); loan.setRemainingPrincipal(new BigDecimal("1000.00")); return loan; }
    private LoanApplicationRequest application() { LoanApplicationRequest request = new LoanApplicationRequest(); request.setCustomerNumber("CUST000003"); request.setLoanType(LoanType.PERSONAL); request.setPrincipal(new BigDecimal("1000.00")); request.setAnnualInterestRate(new BigDecimal("12.50")); request.setTermMonths(12); return request; }
    private LoanDecisionRequest decision(String reason) { LoanDecisionRequest request = new LoanDecisionRequest(); request.setPerformedBy("OPS-5001"); request.setReason(reason); return request; }
    private LoanDisbursementRequest disbursement() { LoanDisbursementRequest request = new LoanDisbursementRequest(); request.setAccountNumber("ACC000101"); request.setPerformedBy("OPS-5001"); request.setReason("Approved loan disbursement."); return request; }
}
