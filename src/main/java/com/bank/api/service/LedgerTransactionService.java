package com.bank.api.service;

import com.bank.api.dto.FundTransferRequest;
import com.bank.api.dto.FundTransferResponse;
import com.bank.api.dto.LedgerTransactionResponse;
import com.bank.api.dto.MoneyTransactionRequest;
import com.bank.api.dto.TransactionHistoryResponse;
import com.bank.api.exception.AccountNotFoundException;
import com.bank.api.exception.InvalidTransactionException;
import com.bank.api.exception.TransactionWorkflowException;
import com.bank.api.model.Account;
import com.bank.api.model.AccountStatus;
import com.bank.api.model.LedgerTransaction;
import com.bank.api.model.LedgerTransactionType;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.LedgerTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerTransactionService {

    private static final String ACCOUNT_NUMBER_REGEX = "^ACC\\d{6}$";
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    private final AccountRepository accountRepository;
    private final LedgerTransactionRepository ledgerTransactionRepository;

    public LedgerTransactionService(AccountRepository accountRepository,
            LedgerTransactionRepository ledgerTransactionRepository) {
        this.accountRepository = accountRepository;
        this.ledgerTransactionRepository = ledgerTransactionRepository;
    }

    @Transactional
    public LedgerTransactionResponse deposit(String idempotencyKey, MoneyTransactionRequest request) {
        Optional<LedgerTransaction> previousTransaction = findPriorTransaction(idempotencyKey);
        if (previousTransaction.isPresent()) {
            return priorMoneyResponse(previousTransaction.get(), LedgerTransactionType.DEPOSIT);
        }
        validateMoneyRequest(request);
        Account account = findLockedAccount(normalizeAccountNumber(request.getAccountNumber()));
        requireActive(account, "Deposits");
        BigDecimal balanceAfter = account.getBalance().add(request.getAmount());
        account.setBalance(balanceAfter);
        accountRepository.save(account);
        LedgerTransaction transaction = saveTransaction(account, LedgerTransactionType.DEPOSIT,
                request.getAmount(), balanceAfter, null, idempotencyKey, trimOptional(request.getDescription()), null);
        return toResponse(transaction);
    }

    @Transactional
    public LedgerTransactionResponse withdraw(String idempotencyKey, MoneyTransactionRequest request) {
        Optional<LedgerTransaction> previousTransaction = findPriorTransaction(idempotencyKey);
        if (previousTransaction.isPresent()) {
            return priorMoneyResponse(previousTransaction.get(), LedgerTransactionType.WITHDRAWAL);
        }
        validateMoneyRequest(request);
        Account account = findLockedAccount(normalizeAccountNumber(request.getAccountNumber()));
        requireActive(account, "Withdrawals");
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new TransactionWorkflowException("Insufficient account balance for withdrawal.");
        }
        BigDecimal balanceAfter = account.getBalance().subtract(request.getAmount());
        account.setBalance(balanceAfter);
        accountRepository.save(account);
        LedgerTransaction transaction = saveTransaction(account, LedgerTransactionType.WITHDRAWAL,
                request.getAmount(), balanceAfter, null, idempotencyKey, trimOptional(request.getDescription()), null);
        return toResponse(transaction);
    }

    @Transactional
    public FundTransferResponse transfer(String idempotencyKey, FundTransferRequest request) {
        Optional<LedgerTransaction> previousTransaction = findPriorTransaction(idempotencyKey);
        if (previousTransaction.isPresent()) {
            return priorTransferResponse(previousTransaction.get());
        }
        validateTransferRequest(request);
        String sourceAccountNumber = normalizeAccountNumber(request.getSourceAccountNumber());
        String destinationAccountNumber = normalizeAccountNumber(request.getDestinationAccountNumber());
        if (sourceAccountNumber.equals(destinationAccountNumber)) {
            throw new InvalidTransactionException("Source and destination accounts must be different.");
        }

        Account firstLockedAccount = findLockedAccount(
                sourceAccountNumber.compareTo(destinationAccountNumber) < 0 ? sourceAccountNumber : destinationAccountNumber);
        Account secondLockedAccount = findLockedAccount(
                sourceAccountNumber.compareTo(destinationAccountNumber) < 0 ? destinationAccountNumber : sourceAccountNumber);
        Account sourceAccount = sourceAccountNumber.equals(firstLockedAccount.getAccountNumber())
                ? firstLockedAccount : secondLockedAccount;
        Account destinationAccount = destinationAccountNumber.equals(firstLockedAccount.getAccountNumber())
                ? firstLockedAccount : secondLockedAccount;
        requireActive(sourceAccount, "Transfers");
        requireActive(destinationAccount, "Transfers");
        if (!sourceAccount.getCurrency().equals(destinationAccount.getCurrency())) {
            throw new TransactionWorkflowException("Cross-currency transfers are not supported.");
        }
        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new TransactionWorkflowException("Insufficient account balance for transfer.");
        }

        BigDecimal sourceBalanceAfter = sourceAccount.getBalance().subtract(request.getAmount());
        BigDecimal destinationBalanceAfter = destinationAccount.getBalance().add(request.getAmount());
        sourceAccount.setBalance(sourceBalanceAfter);
        destinationAccount.setBalance(destinationBalanceAfter);
        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);
        String transferReference = "TRF-" + UUID.randomUUID();
        String description = trimOptional(request.getDescription());
        LedgerTransaction debit = saveTransaction(sourceAccount, LedgerTransactionType.TRANSFER_DEBIT,
                request.getAmount(), sourceBalanceAfter, destinationAccount.getAccountNumber(), idempotencyKey,
                description, transferReference);
        saveTransaction(destinationAccount, LedgerTransactionType.TRANSFER_CREDIT,
                request.getAmount(), destinationBalanceAfter, sourceAccount.getAccountNumber(), null,
                description, transferReference);
        return transferResponse(debit, destinationBalanceAfter);
    }

    @Transactional(readOnly = true)
    public TransactionHistoryResponse getTransactionHistory(String accountNumber, int page, int size, String type) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidTransactionException("Page must be zero or greater and size must be between 1 and 100.");
        }
        Account account = findAccount(normalizeAccountNumber(accountNumber));
        Pageable pageable = PageRequest.of(page, size);
        Page<LedgerTransaction> transactions = isBlank(type)
                ? ledgerTransactionRepository.findByAccount_IdOrderByOccurredAtDesc(account.getId(), pageable)
                : ledgerTransactionRepository.findByAccount_IdAndTypeOrderByOccurredAtDesc(account.getId(),
                        parseTransactionType(type), pageable);
        TransactionHistoryResponse response = new TransactionHistoryResponse();
        response.setTransactions(transactions.getContent().stream().map(this::toResponse).collect(Collectors.toList()));
        response.setPage(transactions.getNumber());
        response.setSize(transactions.getSize());
        response.setTotalElements(transactions.getTotalElements());
        response.setTotalPages(transactions.getTotalPages());
        return response;
    }

    private Optional<LedgerTransaction> findPriorTransaction(String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        return ledgerTransactionRepository.findByIdempotencyKey(idempotencyKey.trim());
    }

    private LedgerTransactionResponse priorMoneyResponse(LedgerTransaction transaction,
            LedgerTransactionType expectedType) {
        if (transaction.getType() != expectedType) {
            throw new TransactionWorkflowException("Idempotency key has already been used for another operation.");
        }
        return toResponse(transaction);
    }

    private FundTransferResponse priorTransferResponse(LedgerTransaction debit) {
        if (debit.getType() != LedgerTransactionType.TRANSFER_DEBIT || debit.getTransferReference() == null) {
            throw new TransactionWorkflowException("Idempotency key has already been used for another operation.");
        }
        LedgerTransaction credit = ledgerTransactionRepository.findByTransferReferenceAndType(
                debit.getTransferReference(), LedgerTransactionType.TRANSFER_CREDIT)
                .orElseThrow(() -> new TransactionWorkflowException("Transfer ledger is incomplete."));
        return transferResponse(debit, credit.getBalanceAfter());
    }

    private LedgerTransaction saveTransaction(Account account, LedgerTransactionType type, BigDecimal amount,
            BigDecimal balanceAfter, String counterpartyAccountNumber, String idempotencyKey, String description,
            String transferReference) {
        LedgerTransaction transaction = new LedgerTransaction();
        transaction.setReference("TXN-" + UUID.randomUUID());
        transaction.setTransferReference(transferReference);
        transaction.setAccount(account);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setCurrency(account.getCurrency());
        transaction.setCounterpartyAccountNumber(counterpartyAccountNumber);
        transaction.setIdempotencyKey(idempotencyKey == null ? null : idempotencyKey.trim());
        transaction.setDescription(description);
        transaction.setOccurredAt(LocalDateTime.now());
        return ledgerTransactionRepository.save(transaction);
    }

    private Account findLockedAccount(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private Account findAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private void validateMoneyRequest(MoneyTransactionRequest request) {
        if (request == null) {
            throw new InvalidTransactionException("Transaction request is mandatory.");
        }
        normalizeAccountNumber(request.getAccountNumber());
        validateAmount(request.getAmount());
        validateDescription(request.getDescription());
    }

    private void validateTransferRequest(FundTransferRequest request) {
        if (request == null) {
            throw new InvalidTransactionException("Transfer request is mandatory.");
        }
        normalizeAccountNumber(request.getSourceAccountNumber());
        normalizeAccountNumber(request.getDestinationAccountNumber());
        validateAmount(request.getAmount());
        validateDescription(request.getDescription());
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (isBlank(idempotencyKey) || idempotencyKey.trim().length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new InvalidTransactionException("Idempotency-Key must contain between 1 and "
                    + IDEMPOTENCY_KEY_MAX_LENGTH + " characters.");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new InvalidTransactionException("Amount must be greater than zero with at most two decimal places.");
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.trim().length() > DESCRIPTION_MAX_LENGTH) {
            throw new InvalidTransactionException("Description cannot exceed " + DESCRIPTION_MAX_LENGTH + " characters.");
        }
    }

    private void requireActive(Account account, String operation) {
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new TransactionWorkflowException(operation + " require an ACTIVE account.");
        }
        if (account.getBalance() == null) {
            throw new TransactionWorkflowException("Account has no available balance.");
        }
    }

    private LedgerTransactionType parseTransactionType(String type) {
        try {
            return LedgerTransactionType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidTransactionException("Unsupported transaction type.");
        }
    }

    private String normalizeAccountNumber(String accountNumber) {
        if (isBlank(accountNumber)) {
            throw new InvalidTransactionException("Account number is mandatory.");
        }
        String normalized = accountNumber.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches(ACCOUNT_NUMBER_REGEX)) {
            throw new InvalidTransactionException("Invalid account number format.");
        }
        return normalized;
    }

    private FundTransferResponse transferResponse(LedgerTransaction debit, BigDecimal destinationBalanceAfter) {
        FundTransferResponse response = new FundTransferResponse();
        response.setReference(debit.getTransferReference());
        response.setStatus("BOOKED");
        response.setSourceAccountNumber(debit.getAccount().getAccountNumber());
        response.setSourceBalanceAfter(debit.getBalanceAfter());
        response.setDestinationAccountNumber(debit.getCounterpartyAccountNumber());
        response.setDestinationBalanceAfter(destinationBalanceAfter);
        response.setCurrency(debit.getCurrency());
        return response;
    }

    private LedgerTransactionResponse toResponse(LedgerTransaction transaction) {
        LedgerTransactionResponse response = new LedgerTransactionResponse();
        response.setReference(transaction.getReference());
        response.setAccountNumber(transaction.getAccount().getAccountNumber());
        response.setType(transaction.getType().name());
        response.setAmount(transaction.getAmount());
        response.setBalanceAfter(transaction.getBalanceAfter());
        response.setCurrency(transaction.getCurrency());
        response.setCounterpartyAccountNumber(transaction.getCounterpartyAccountNumber());
        response.setDescription(transaction.getDescription());
        response.setOccurredAt(transaction.getOccurredAt());
        return response;
    }

    private String trimOptional(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
