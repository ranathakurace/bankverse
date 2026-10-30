package com.bank.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.bank.api.model.Account;
import com.bank.api.model.AccountType;
import com.bank.api.model.Customer;

public interface AccountRepository extends JpaRepository<Account, Long>{

	Optional<Account> findByAccountNumber(String accountNumber);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select account from Account account where account.accountNumber = :accountNumber")
	Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

	List<Account> findByCustomer_CustomerNumberOrderByCreatedAtDesc(String customerNumber);

	boolean existsByCustomerAndAccountType(
	        Customer customer,
	        AccountType accountType);
	@Query(value = "SELECT nextval('account_sequence')", nativeQuery = true)
	Long getNextAccountSequence();
}
