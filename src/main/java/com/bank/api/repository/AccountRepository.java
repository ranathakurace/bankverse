package com.bank.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.bank.api.model.Account;
import com.bank.api.model.AccountType;
import com.bank.api.model.Customer;

public interface AccountRepository extends JpaRepository<Account, Long>{

	Optional<Account> findByAccountNumber(String accountNumber);

	boolean existsByCustomerAndAccountType(
	        Customer customer,
	        AccountType accountType);
	@Query(value = "SELECT nextval('account_sequence')", nativeQuery = true)
	Long getNextAccountSequence();
}
