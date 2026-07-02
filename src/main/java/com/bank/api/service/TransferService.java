package com.bank.api.service;
import com.bank.api.dto.TransferRequest;
import com.bank.api.dto.TransferResponse;
import com.bank.api.exception.AccountNotFoundException;
import com.bank.api.exception.TransferFailedException;
import com.bank.api.idempotency.IdempotencyStore;
import com.bank.api.model.Account;
import com.bank.api.model.AccountStatus;
import com.bank.api.repository.AccountRepository;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

@Service
public class TransferService {

    private final AccountRepository repository;

    public TransferService(AccountRepository repository) {
        this.repository = repository;
    }

    /**
     * Atomic & Idempotent Money Transfer
     */
    public TransferResponse transfer(String idempotencyKey,
                                     TransferRequest request) {

        // 1️⃣ Idempotency check
        if (IdempotencyStore.exists(idempotencyKey)) {
            return IdempotencyStore.get(idempotencyKey);
        }

        // 2️⃣ Fetch accounts
        Long fromAccountId = request.getFromAccountId().longValue();
        Long toAccountId = request.getToAccountId().longValue();

        Account from = repository.findById(fromAccountId)
                .orElseThrow(() -> new TransferFailedException(
                        "Source account not found"));

        Account to = repository.findById(toAccountId)
                .orElseThrow(() -> new TransferFailedException(
                        "Destination account not found"));

       

        // 3️⃣ Business validations
        if (from.getId() == to.getId()) {
            throw new TransferFailedException("Cannot transfer to same account");
        }

        if (from.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new TransferFailedException("Source account is not active");
        }

        if (from.getBalance().compareTo(BigDecimal.valueOf(request.getAmount())) < 0) {
            throw new TransferFailedException("Insufficient balance");
        }

        // 4️⃣ Take snapshot for rollback
        BigDecimal originalFromBalance = from.getBalance();
        BigDecimal originalToBalance = to.getBalance();

        try {

            // 5 Debit source account
        	from.setBalance(
        		    originalFromBalance.subtract(BigDecimal.valueOf(request.getAmount()))
        		);

            repository.save(from);

            // 🔴 OPTIONAL: simulate failure for learning/testing
            // if (request.getAmount() > 10000) {
            //     throw new RuntimeException("Simulated credit failure");
            // }

            // 6️⃣ Credit destination account
            to.setBalance(
            	    originalToBalance.add(BigDecimal.valueOf(request.getAmount()))
            	);

            repository.save(to);

            // 7️⃣ Prepare response
            TransferResponse response = new TransferResponse(
                    "SUCCESS",
                    "Transfer completed",
                    from.getBalance(),
                    to.getBalance()
            );

            // 8️⃣ Save idempotent result
            IdempotencyStore.save(idempotencyKey, response);

            return response;

        } catch (Exception ex) {

            // Rollback balances
            from.setBalance(originalFromBalance);
            to.setBalance(originalToBalance);

            repository.save(from);
            repository.save(to);

            throw new TransferFailedException(
                    "Transfer rolled back due to error: " + ex.getMessage());
        }
        }
    }

