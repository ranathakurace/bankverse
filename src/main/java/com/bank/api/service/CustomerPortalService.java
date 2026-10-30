package com.bank.api.service;

import com.bank.api.dto.AccountResponse;
import com.bank.api.dto.CustomerOnlineAccessRequest;
import com.bank.api.dto.CustomerOnlineLoginRequest;
import com.bank.api.dto.FundTransferRequest;
import com.bank.api.dto.FundTransferResponse;
import com.bank.api.dto.PasswordChangeRequest;
import com.bank.api.dto.TransactionHistoryResponse;
import com.bank.api.exception.CustomerPortalException;
import com.bank.api.model.Account;
import com.bank.api.model.Customer;
import com.bank.api.model.CustomerOnlineAccess;
import com.bank.api.model.CustomerStatus;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.CustomerOnlineAccessRepository;
import com.bank.api.repository.CustomerRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerPortalService {
    private final CustomerRepository customers; private final CustomerOnlineAccessRepository access; private final AccountRepository accounts;
    private final LedgerTransactionService ledger; private final PasswordEncoder encoder;
    public CustomerPortalService(CustomerRepository customers, CustomerOnlineAccessRepository access, AccountRepository accounts, LedgerTransactionService ledger, PasswordEncoder encoder) { this.customers = customers; this.access = access; this.accounts = accounts; this.ledger = ledger; this.encoder = encoder; }
    @Transactional public Customer enroll(CustomerOnlineAccessRequest request) { if (request == null) throw new CustomerPortalException("Online banking enrollment request is mandatory."); Customer customer = eligible(request.getCustomerNumber()); if (blank(request.getEmail()) || !customer.getEmail().equalsIgnoreCase(request.getEmail().trim())) throw new CustomerPortalException("Customer number and registered email do not match."); if (access.findByCustomer_CustomerNumber(customer.getCustomerNumber()).isPresent()) throw new CustomerPortalException("Online access is already enrolled for this customer."); validatePassword(request.getPassword()); CustomerOnlineAccess enrollment = new CustomerOnlineAccess(); enrollment.setCustomer(customer); enrollment.setPasswordHash(encoder.encode(request.getPassword())); enrollment.setActive(true); enrollment.setCreatedAt(LocalDateTime.now()); enrollment.setUpdatedAt(LocalDateTime.now()); access.save(enrollment); return customer; }
    @Transactional(readOnly = true) public Customer authenticate(CustomerOnlineLoginRequest request) { if (request == null || blank(request.getCustomerNumber()) || blank(request.getPassword())) throw new CustomerPortalException("Invalid customer credentials."); CustomerOnlineAccess enrollment = access.findByCustomer_CustomerNumber(request.getCustomerNumber().trim().toUpperCase(Locale.ROOT)).orElseThrow(() -> new CustomerPortalException("Invalid customer credentials.")); if (!enrollment.isActive() || !encoder.matches(request.getPassword(), enrollment.getPasswordHash())) throw new CustomerPortalException("Invalid customer credentials."); return eligible(enrollment.getCustomer().getCustomerNumber()); }
    @Transactional(readOnly = true) public List<AccountResponse> accounts(String customerNumber) { return accounts.findByCustomer_CustomerNumberOrderByCreatedAtDesc(customerNumber).stream().map(this::account).collect(Collectors.toList()); }
    @Transactional(readOnly = true) public TransactionHistoryResponse history(String customerNumber, String accountNumber, int page, int size) { assertOwned(customerNumber, accountNumber); return ledger.getTransactionHistory(accountNumber, page, size, null); }
    @Transactional public FundTransferResponse transfer(String customerNumber, String key, FundTransferRequest request) { if (request == null) throw new CustomerPortalException("Transfer request is mandatory."); assertOwned(customerNumber, request.getSourceAccountNumber()); return ledger.transfer(key, request); }
    @Transactional public void changePassword(String customerNumber, PasswordChangeRequest request) { CustomerOnlineAccess enrollment = access.findByCustomer_CustomerNumber(customerNumber).orElseThrow(() -> new CustomerPortalException("Online access was not found.")); if (request == null || !encoder.matches(request.getCurrentPassword(), enrollment.getPasswordHash())) throw new CustomerPortalException("Current password is incorrect."); validatePassword(request.getNewPassword()); enrollment.setPasswordHash(encoder.encode(request.getNewPassword())); enrollment.setUpdatedAt(LocalDateTime.now()); access.save(enrollment); }
    private Customer eligible(String number) { if (blank(number)) throw new CustomerPortalException("Customer number is mandatory."); Customer customer = customers.findByCustomerNumber(number.trim().toUpperCase(Locale.ROOT)).orElseThrow(() -> new CustomerPortalException("Customer was not found.")); if (customer.getCustomerStatus() != CustomerStatus.ACTIVE || !"COMPLETED".equalsIgnoreCase(customer.getKycStatus())) throw new CustomerPortalException("Online banking is available only for active customers with completed KYC."); return customer; }
    private void assertOwned(String customerNumber, String accountNumber) { Account account = accounts.findByAccountNumber(accountNumber == null ? "" : accountNumber.trim().toUpperCase(Locale.ROOT)).orElseThrow(() -> new CustomerPortalException("Account was not found.")); if (account.getCustomer() == null || !customerNumber.equals(account.getCustomer().getCustomerNumber())) throw new CustomerPortalException("You can only access your own accounts."); }
    private AccountResponse account(Account value) { return new AccountResponse(value.getAccountNumber(), value.getCustomer().getCustomerNumber(), value.getAccountType(), value.getBalance(), value.getCurrency(), value.getAccountStatus()); }
    private void validatePassword(String value) { if (blank(value) || value.length() < 12 || !value.matches(".*[A-Z].*") || !value.matches(".*[a-z].*") || !value.matches(".*[0-9].*")) throw new CustomerPortalException("Password must have at least 12 characters with upper-case, lower-case and numeric characters."); }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
