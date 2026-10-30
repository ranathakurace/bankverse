package com.bank.api.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.api.dto.AccountAuditResponse;
import com.bank.api.dto.AccountRequest;
import com.bank.api.dto.AccountResponse;
import com.bank.api.dto.AccountStatementResponse;
import com.bank.api.dto.AccountStatusChangeRequest;
import com.bank.api.exception.AccountAlreadyExistsException;
import com.bank.api.exception.CustomerNotFoundException;
import com.bank.api.exception.AccountNotFoundException;
import com.bank.api.exception.AccountWorkflowException;
import com.bank.api.exception.InvalidAccountException;
import com.bank.api.exception.InvalidCustomerException;
import com.bank.api.model.Account;
import com.bank.api.model.AccountAuditEvent;
import com.bank.api.model.AccountStatus;
import com.bank.api.model.Customer;
import com.bank.api.model.CustomerStatus;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.AccountAuditEventRepository;
import com.bank.api.repository.CustomerRepository;

@Service
public class AccountService {

    private static final String CUSTOMER_NUMBER_REGEX = "^CUST\\d{6}$";
    private static final String ACCOUNT_NUMBER_REGEX = "^ACC\\d{6}$";
    private static final int ACTOR_MAX_LENGTH = 100;
    private static final int REASON_MAX_LENGTH = 500;

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final AccountAuditEventRepository accountAuditEventRepository;

    public AccountService(CustomerRepository customerRepository, AccountRepository accountRepository,
            AccountAuditEventRepository accountAuditEventRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.accountAuditEventRepository = accountAuditEventRepository;
    }

    @Transactional
    public AccountResponse createAccount(AccountRequest request) {

    	/*
    	 * =========================================================
    	 * Step 1 : Find Customer
    	 * =========================================================
    	 */
        validateAccountRequest(request);
        String customerNumber = normalizeCustomerNumber(request.getCustomerNumber());
        Customer customer = customerRepository
                .findByCustomerNumber(customerNumber)
                .orElseThrow(() ->
                        new CustomerNotFoundException(
                                "customer number",
                                customerNumber));

        /*
         * =========================================================
         * Step 2 : Validate Customer Status
         * =========================================================
         */
        if (customer.getCustomerStatus() != CustomerStatus.ACTIVE) {
            throw new InvalidCustomerException(
                    "Customer is not eligible to open an account.");
        }
        /*
         * =========================================================
         * Step 2.1 : Validate Duplicate Account
         * =========================================================
         */
        boolean accountExists = accountRepository
                .existsByCustomerAndAccountType(
                        customer,
                        request.getAccountType());

        if (accountExists) {
            throw new AccountAlreadyExistsException(
                    "Customer already has a "
                    + request.getAccountType()
                    + " account.");
        }
        /*
         * =========================================================
         * Step 3 : Generate Account Number
         * =========================================================
         */
        Long sequenceNumber = accountRepository.getNextAccountSequence();

        String accountNumber =
                String.format("ACC%06d", sequenceNumber);
        /*
         * =========================================================
         * Step 4 : Create Account Entity
         * =========================================================
         */
        Account account = new Account();

        account.setAccountNumber(accountNumber);
        account.setCustomer(customer);
        account.setAccountType(request.getAccountType());
        /*
         * =========================================================
         * Step 4.1 : Initialize Enterprise Defaults
         * =========================================================
         */

        account.setBalance(BigDecimal.ZERO);

        account.setCurrency(normalizeCurrency(request.getCurrency()));

        account.setAccountStatus(AccountStatus.ACTIVE);

        account.setCreatedAt(LocalDateTime.now());
        
        /*
         * =========================================================
         * Step 5 : Save Account
         * =========================================================
         */

        Account savedAccount = accountRepository.save(account);
        recordAudit(savedAccount, "ACCOUNT_OPENED", null, AccountStatus.ACTIVE, "SYSTEM",
                "Account opened for an active customer.", LocalDateTime.now());
        
        /*
         * =========================================================
         * Step 6 : Build Response
         * =========================================================
         */

        return new AccountResponse(
                savedAccount.getAccountNumber(),
                savedAccount.getCustomer().getCustomerNumber(),
                savedAccount.getAccountType(),
                savedAccount.getBalance(),
                savedAccount.getCurrency(),
                savedAccount.getAccountStatus()
        );

    }
    
   

    @Transactional(readOnly = true)
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        return toResponse(findAccount(accountNumber));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomerNumber(String customerNumber) {
        String normalizedCustomerNumber = normalizeCustomerNumber(customerNumber);
        customerRepository.findByCustomerNumber(normalizedCustomerNumber)
                .orElseThrow(() -> new CustomerNotFoundException("customer number", normalizedCustomerNumber));
        return accountRepository.findByCustomer_CustomerNumberOrderByCreatedAtDesc(normalizedCustomerNumber).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public AccountResponse freezeAccount(String accountNumber, AccountStatusChangeRequest request) {
        Account account = findAccount(accountNumber);
        validateStatusChangeRequest(request);
        changeStatus(account, AccountStatus.ACTIVE, AccountStatus.FROZEN, "ACCOUNT_FROZEN", request);
        return toResponse(account);
    }

    @Transactional
    public AccountResponse unfreezeAccount(String accountNumber, AccountStatusChangeRequest request) {
        Account account = findAccount(accountNumber);
        validateStatusChangeRequest(request);
        changeStatus(account, AccountStatus.FROZEN, AccountStatus.ACTIVE, "ACCOUNT_UNFROZEN", request);
        return toResponse(account);
    }

    @Transactional
    public AccountResponse closeAccount(String accountNumber, AccountStatusChangeRequest request) {
        Account account = findAccount(accountNumber);
        validateStatusChangeRequest(request);
        if (account.getAccountStatus() != AccountStatus.ACTIVE && account.getAccountStatus() != AccountStatus.FROZEN) {
            throw new AccountWorkflowException("Only ACTIVE or FROZEN accounts can be closed.");
        }
        if (account.getBalance() == null || account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new AccountWorkflowException("Account must have a zero balance before it can be closed.");
        }
        AccountStatus previousStatus = account.getAccountStatus();
        account.setAccountStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
        recordAudit(account, "ACCOUNT_CLOSED", previousStatus, AccountStatus.CLOSED,
                request.getPerformedBy().trim(), request.getReason().trim(), LocalDateTime.now());
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountStatementResponse getAccountStatement(String accountNumber) {
        Account account = findAccount(accountNumber);
        AccountStatementResponse response = new AccountStatementResponse();
        response.setAccountNumber(account.getAccountNumber());
        response.setCustomerNumber(account.getCustomer().getCustomerNumber());
        response.setAccountType(account.getAccountType().name());
        response.setAccountStatus(account.getAccountStatus().name());
        response.setCurrency(account.getCurrency());
        response.setCurrentBalance(account.getBalance());
        response.setAccountOpenedAt(account.getCreatedAt());
        response.setStatementGeneratedAt(LocalDateTime.now());
        response.setLifecycleEvents(accountAuditEventRepository.findByAccount_IdOrderByOccurredAtAsc(account.getId())
                .stream().map(this::toAuditResponse).collect(Collectors.toList()));
        return response;
    }

    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

    private Account findAccount(String accountNumber) {
        String normalizedAccountNumber = normalizeAccountNumber(accountNumber);
        return accountRepository.findByAccountNumber(normalizedAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException(normalizedAccountNumber));
    }

    private void changeStatus(Account account, AccountStatus expectedStatus, AccountStatus targetStatus,
            String action, AccountStatusChangeRequest request) {
        if (account.getAccountStatus() != expectedStatus) {
            throw new AccountWorkflowException("Account must be in " + expectedStatus + " status for this operation.");
        }
        account.setAccountStatus(targetStatus);
        accountRepository.save(account);
        recordAudit(account, action, expectedStatus, targetStatus, request.getPerformedBy().trim(),
                request.getReason().trim(), LocalDateTime.now());
    }

    private void recordAudit(Account account, String action, AccountStatus previousStatus,
            AccountStatus newStatus, String performedBy, String reason, LocalDateTime occurredAt) {
        AccountAuditEvent event = new AccountAuditEvent();
        event.setAccount(account);
        event.setAction(action);
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setPerformedBy(performedBy);
        event.setReason(reason);
        event.setOccurredAt(occurredAt);
        accountAuditEventRepository.save(event);
    }

    private void validateAccountRequest(AccountRequest request) {
        if (request == null) {
            throw new InvalidAccountException("Account request is mandatory.");
        }
        normalizeCustomerNumber(request.getCustomerNumber());
        if (request.getAccountType() == null) {
            throw new InvalidAccountException("Account type is mandatory.");
        }
        normalizeCurrency(request.getCurrency());
    }

    private void validateStatusChangeRequest(AccountStatusChangeRequest request) {
        if (request == null) {
            throw new InvalidAccountException("Account status change request is mandatory.");
        }
        if (isBlank(request.getPerformedBy()) || request.getPerformedBy().trim().length() > ACTOR_MAX_LENGTH) {
            throw new InvalidAccountException("Performed by must contain between 1 and " + ACTOR_MAX_LENGTH + " characters.");
        }
        if (isBlank(request.getReason())) {
            throw new InvalidAccountException("Reason is mandatory for this account status change.");
        }
        if (request.getReason().trim().length() > REASON_MAX_LENGTH) {
            throw new InvalidAccountException("Reason cannot exceed " + REASON_MAX_LENGTH + " characters.");
        }
    }

    private String normalizeCustomerNumber(String customerNumber) {
        if (isBlank(customerNumber)) {
            throw new InvalidAccountException("Customer number is mandatory.");
        }
        String normalized = customerNumber.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches(CUSTOMER_NUMBER_REGEX)) {
            throw new InvalidAccountException("Invalid customer number format.");
        }
        return normalized;
    }

    private String normalizeAccountNumber(String accountNumber) {
        if (isBlank(accountNumber)) {
            throw new InvalidAccountException("Account number is mandatory.");
        }
        String normalized = accountNumber.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches(ACCOUNT_NUMBER_REGEX)) {
            throw new InvalidAccountException("Invalid account number format.");
        }
        return normalized;
    }

    private String normalizeCurrency(String currency) {
        if (isBlank(currency)) {
            throw new InvalidAccountException("Currency is mandatory.");
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        try {
            return Currency.getInstance(normalized).getCurrencyCode();
        } catch (IllegalArgumentException exception) {
            throw new InvalidAccountException("Currency must be a valid ISO-4217 code.");
        }
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getAccountNumber(), account.getCustomer().getCustomerNumber(),
                account.getAccountType(), account.getBalance(), account.getCurrency(), account.getAccountStatus());
    }

    private AccountAuditResponse toAuditResponse(AccountAuditEvent event) {
        AccountAuditResponse response = new AccountAuditResponse();
        response.setAction(event.getAction());
        response.setPreviousStatus(event.getPreviousStatus() == null ? null : event.getPreviousStatus().name());
        response.setNewStatus(event.getNewStatus().name());
        response.setPerformedBy(event.getPerformedBy());
        response.setReason(event.getReason());
        response.setOccurredAt(event.getOccurredAt());
        return response;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
