package com.bank.api.service;

import com.bank.api.dto.TransferRequest;
import com.bank.api.dto.TransferResponse;
import com.bank.api.exception.AccountNotFoundException;
import com.bank.api.exception.TransferFailedException;
import com.bank.api.idempotency.IdempotencyStore;
import com.bank.api.model.Account;
import com.bank.api.repository.AccountRepository;
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
        Account from = repository.findById(request.getFromAccountId());
        Account to = repository.findById(request.getToAccountId());

        if (from == null) {
            throw new AccountNotFoundException(request.getFromAccountId());
        }
        if (to == null) {
            throw new AccountNotFoundException(request.getToAccountId());
        }

        // 3️⃣ Business validations
        if (from.getAccountId() == to.getAccountId()) {
            throw new TransferFailedException("Cannot transfer to same account");
        }

        if (!"ACTIVE".equalsIgnoreCase(from.getStatus())) {
            throw new TransferFailedException("Source account is not active");
        }

        if (from.getBalance() < request.getAmount()) {
            throw new TransferFailedException("Insufficient balance");
        }

        // 4️⃣ Take snapshot for rollback
        double originalFromBalance = from.getBalance();
        double originalToBalance = to.getBalance();

        try {
            // 5️⃣ Debit source account
        	Account debitedFrom = new Account(
        		    from.getAccountId(),
        		    from.getCustomerId(),
        		    from.getAccountType(),
        		    originalFromBalance - request.getAmount(),
        		    from.getCurrency(),
        		    from.getStatus()
        		);
            repository.update(debitedFrom);

            // 🔴 OPTIONAL: simulate failure for learning/testing
            // if (request.getAmount() > 10000) {
            //     throw new RuntimeException("Simulated credit failure");
            // }

            // 6️⃣ Credit destination account
            Account creditedTo = new Account(
            	    to.getAccountId(),
            	    to.getCustomerId(),
            	    to.getAccountType(),
            	    originalToBalance + request.getAmount(),
            	    to.getCurrency(),
            	    to.getStatus()
            	);
            repository.update(creditedTo);

            // 7️⃣ Prepare response
            TransferResponse response = new TransferResponse(
                    "SUCCESS",
                    "Transfer completed",
                    debitedFrom.getBalance(),
                    creditedTo.getBalance()
            );

            // 8️⃣ Save idempotent result
            IdempotencyStore.save(idempotencyKey, response);

            return response;

        } catch (Exception ex) {

            // 9️⃣ ROLLBACK on any failure
        	repository.update(new Account(
        		    from.getAccountId(),
        		    from.getCustomerId(),
        		    from.getAccountType(),
        		    originalFromBalance,
        		    from.getCurrency(),
        		    from.getStatus()
        		));

        	repository.update(new Account(
        		    to.getAccountId(),
        		    to.getCustomerId(),
        		    to.getAccountType(),
        		    originalToBalance,
        		    to.getCurrency(),
        		    to.getStatus()
        		));

            throw new TransferFailedException(
                    "Transfer rolled back due to error: " + ex.getMessage()
            );
        }
    }
}
