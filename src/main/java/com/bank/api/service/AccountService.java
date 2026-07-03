package com.bank.api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bank.api.dto.AccountRequest;
import com.bank.api.dto.AccountResponse;
import com.bank.api.exception.AccountAlreadyExistsException;
import com.bank.api.exception.CustomerNotFoundException;
import com.bank.api.exception.InvalidCustomerException;
import com.bank.api.model.Account;
import com.bank.api.model.AccountStatus;
import com.bank.api.model.Customer;
import com.bank.api.model.CustomerStatus;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.CustomerRepository;

@Service
public class AccountService {
	@Autowired
	private CustomerRepository customerRepository;
    @Autowired
    private AccountRepository accountRepository;

    public AccountResponse createAccount(AccountRequest request) {

    	/*
    	 * =========================================================
    	 * Step 1 : Find Customer
    	 * =========================================================
    	 */
        Customer customer = customerRepository
                .findByCustomerNumber(request.getCustomerNumber())
                .orElseThrow(() ->
                        new CustomerNotFoundException(
                                "customer number",
                                request.getCustomerNumber()));

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

        account.setBalance(java.math.BigDecimal.ZERO);

        account.setCurrency(request.getCurrency());

        account.setAccountStatus(AccountStatus.ACTIVE);

        account.setCreatedAt(java.time.LocalDateTime.now());
        
        /*
         * =========================================================
         * Step 5 : Save Account
         * =========================================================
         */

        Account savedAccount = accountRepository.save(account);
        
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
    
    public Account save(Account account) {
        return accountRepository.save(account);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccount(Long id) {
        return accountRepository.findById(id).orElse(null);
    }

    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }
}