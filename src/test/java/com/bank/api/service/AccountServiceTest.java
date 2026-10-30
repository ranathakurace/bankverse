package com.bank.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

import com.bank.api.dto.AccountRequest;
import com.bank.api.dto.AccountResponse;
import com.bank.api.dto.AccountStatementResponse;
import com.bank.api.dto.AccountStatusChangeRequest;
import com.bank.api.exception.AccountAlreadyExistsException;
import com.bank.api.exception.AccountWorkflowException;
import com.bank.api.exception.InvalidAccountException;
import com.bank.api.exception.InvalidCustomerException;
import com.bank.api.model.Account;
import com.bank.api.model.AccountAuditEvent;
import com.bank.api.model.AccountStatus;
import com.bank.api.model.AccountType;
import com.bank.api.model.Customer;
import com.bank.api.model.CustomerStatus;
import com.bank.api.repository.AccountAuditEventRepository;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.CustomerRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AccountServiceTest {

    private CustomerRepository customerRepository;
    private AccountRepository accountRepository;
    private AccountAuditEventRepository accountAuditEventRepository;
    private AccountService accountService;

    @BeforeMethod
    public void setUp() {
        customerRepository = mock(CustomerRepository.class);
        accountRepository = mock(AccountRepository.class);
        accountAuditEventRepository = mock(AccountAuditEventRepository.class);
        accountService = new AccountService(customerRepository, accountRepository, accountAuditEventRepository);
    }

    @Test
    public void opensAccountForActiveCustomerAndWritesAuditEvent() {
        Customer customer = activeCustomer();
        when(customerRepository.findByCustomerNumber("CUST000003")).thenReturn(Optional.of(customer));
        when(accountRepository.existsByCustomerAndAccountType(customer, AccountType.SAVINGS)).thenReturn(false);
        when(accountRepository.getNextAccountSequence()).thenReturn(12L);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(12L);
            return account;
        });

        AccountResponse response = accountService.createAccount(accountRequest());

        assertEquals(response.getAccountNumber(), "ACC000012");
        assertEquals(response.getAccountStatus(), AccountStatus.ACTIVE);
        assertEquals(response.getCurrency(), "INR");
        verify(accountAuditEventRepository).save(any(AccountAuditEvent.class));
    }

    @Test
    public void preventsAccountOpeningForNonActiveCustomer() {
        Customer customer = activeCustomer();
        customer.setCustomerStatus(CustomerStatus.PENDING_KYC);
        when(customerRepository.findByCustomerNumber("CUST000003")).thenReturn(Optional.of(customer));

        expectThrows(InvalidCustomerException.class, () -> accountService.createAccount(accountRequest()));
    }

    @Test
    public void preventsDuplicateAccountTypeForCustomer() {
        Customer customer = activeCustomer();
        when(customerRepository.findByCustomerNumber("CUST000003")).thenReturn(Optional.of(customer));
        when(accountRepository.existsByCustomerAndAccountType(customer, AccountType.SAVINGS)).thenReturn(true);

        expectThrows(AccountAlreadyExistsException.class, () -> accountService.createAccount(accountRequest()));
    }

    @Test
    public void rejectsInvalidCurrencyBeforeDatabaseAccess() {
        AccountRequest request = accountRequest();
        request.setCurrency("XYZ");

        expectThrows(InvalidAccountException.class, () -> accountService.createAccount(request));
    }

    @Test
    public void freezesActiveAccountAndWritesAuditEvent() {
        Account account = account(AccountStatus.ACTIVE, BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber("ACC000012")).thenReturn(Optional.of(account));

        AccountResponse response = accountService.freezeAccount("acc000012", statusRequest("Suspicious activity review."));

        assertEquals(response.getAccountStatus(), AccountStatus.FROZEN);
        verify(accountRepository).save(account);
        verify(accountAuditEventRepository).save(any(AccountAuditEvent.class));
    }

    @Test
    public void unfreezesFrozenAccount() {
        Account account = account(AccountStatus.FROZEN, BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber("ACC000012")).thenReturn(Optional.of(account));

        AccountResponse response = accountService.unfreezeAccount("ACC000012", statusRequest("Review completed."));

        assertEquals(response.getAccountStatus(), AccountStatus.ACTIVE);
    }

    @Test
    public void closesZeroBalanceAccount() {
        Account account = account(AccountStatus.ACTIVE, BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber("ACC000012")).thenReturn(Optional.of(account));

        AccountResponse response = accountService.closeAccount("ACC000012", statusRequest("Customer requested closure."));

        assertEquals(response.getAccountStatus(), AccountStatus.CLOSED);
        verify(accountAuditEventRepository).save(any(AccountAuditEvent.class));
    }

    @Test
    public void preventsClosingAccountWithOutstandingBalance() {
        Account account = account(AccountStatus.ACTIVE, new BigDecimal("5.00"));
        when(accountRepository.findByAccountNumber("ACC000012")).thenReturn(Optional.of(account));

        expectThrows(AccountWorkflowException.class,
                () -> accountService.closeAccount("ACC000012", statusRequest("Customer requested closure.")));
    }

    @Test
    public void returnsLifecycleStatement() {
        Account account = account(AccountStatus.ACTIVE, BigDecimal.ZERO);
        AccountAuditEvent opened = auditEvent("ACCOUNT_OPENED", null, AccountStatus.ACTIVE);
        AccountAuditEvent frozen = auditEvent("ACCOUNT_FROZEN", AccountStatus.ACTIVE, AccountStatus.FROZEN);
        when(accountRepository.findByAccountNumber("ACC000012")).thenReturn(Optional.of(account));
        when(accountAuditEventRepository.findByAccount_IdOrderByOccurredAtAsc(12L))
                .thenReturn(Arrays.asList(opened, frozen));

        AccountStatementResponse statement = accountService.getAccountStatement("ACC000012");

        assertEquals(statement.getAccountNumber(), "ACC000012");
        assertEquals(statement.getLifecycleEvents().size(), 2);
        assertEquals(statement.getLifecycleEvents().get(1).getNewStatus(), AccountStatus.FROZEN.name());
    }

    private Customer activeCustomer() {
        Customer customer = new Customer();
        customer.setId(3L);
        customer.setCustomerNumber("CUST000003");
        customer.setCustomerStatus(CustomerStatus.ACTIVE);
        return customer;
    }

    private Account account(AccountStatus status, BigDecimal balance) {
        Account account = new Account();
        account.setId(12L);
        account.setAccountNumber("ACC000012");
        account.setCustomer(activeCustomer());
        account.setAccountType(AccountType.SAVINGS);
        account.setAccountStatus(status);
        account.setBalance(balance);
        account.setCurrency("INR");
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }

    private AccountRequest accountRequest() {
        AccountRequest request = new AccountRequest();
        request.setCustomerNumber("cust000003");
        request.setAccountType(AccountType.SAVINGS);
        request.setCurrency("inr");
        return request;
    }

    private AccountStatusChangeRequest statusRequest(String reason) {
        AccountStatusChangeRequest request = new AccountStatusChangeRequest();
        request.setPerformedBy("OPS-3001");
        request.setReason(reason);
        return request;
    }

    private AccountAuditEvent auditEvent(String action, AccountStatus previousStatus, AccountStatus newStatus) {
        AccountAuditEvent event = new AccountAuditEvent();
        event.setAction(action);
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setPerformedBy("OPS-3001");
        event.setReason("Operational action.");
        event.setOccurredAt(LocalDateTime.now());
        return event;
    }
}
