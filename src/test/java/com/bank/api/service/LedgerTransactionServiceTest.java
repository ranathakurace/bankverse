package com.bank.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

import com.bank.api.dto.FundTransferRequest;
import com.bank.api.dto.FundTransferResponse;
import com.bank.api.dto.LedgerTransactionResponse;
import com.bank.api.dto.MoneyTransactionRequest;
import com.bank.api.dto.TransactionHistoryResponse;
import com.bank.api.exception.TransactionWorkflowException;
import com.bank.api.model.Account;
import com.bank.api.model.AccountStatus;
import com.bank.api.model.LedgerTransaction;
import com.bank.api.model.LedgerTransactionType;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.LedgerTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LedgerTransactionServiceTest {

    private AccountRepository accountRepository;
    private LedgerTransactionRepository ledgerTransactionRepository;
    private LedgerTransactionService transactionService;

    @BeforeMethod
    public void setUp() {
        accountRepository = mock(AccountRepository.class);
        ledgerTransactionRepository = mock(LedgerTransactionRepository.class);
        transactionService = new LedgerTransactionService(accountRepository, ledgerTransactionRepository);
        when(ledgerTransactionRepository.save(any(LedgerTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void depositsMoneyAndCreatesPersistentLedgerEntry() {
        Account account = account("ACC000101", "100.00", "INR");
        when(accountRepository.findByAccountNumberForUpdate("ACC000101")).thenReturn(Optional.of(account));

        LedgerTransactionResponse response = transactionService.deposit("deposit-101", moneyRequest("ACC000101", "25.50"));

        assertEquals(response.getType(), LedgerTransactionType.DEPOSIT.name());
        assertEquals(response.getBalanceAfter(), new BigDecimal("125.50"));
        assertEquals(account.getBalance(), new BigDecimal("125.50"));
        verify(accountRepository).save(account);
        verify(ledgerTransactionRepository).save(any(LedgerTransaction.class));
    }

    @Test
    public void returnsPriorDepositForSameIdempotencyKeyWithoutChangingBalance() {
        Account account = account("ACC000101", "100.00", "INR");
        LedgerTransaction previous = ledgerTransaction(account, LedgerTransactionType.DEPOSIT, "125.00");
        when(ledgerTransactionRepository.findByIdempotencyKey("deposit-101")).thenReturn(Optional.of(previous));

        LedgerTransactionResponse response = transactionService.deposit("deposit-101", moneyRequest("ACC000101", "25.00"));

        assertEquals(response.getBalanceAfter(), new BigDecimal("125.00"));
        assertEquals(account.getBalance(), new BigDecimal("100.00"));
        verify(accountRepository, never()).findByAccountNumberForUpdate(any());
    }

    @Test
    public void preventsWithdrawalWhenBalanceIsInsufficient() {
        Account account = account("ACC000101", "10.00", "INR");
        when(accountRepository.findByAccountNumberForUpdate("ACC000101")).thenReturn(Optional.of(account));

        expectThrows(TransactionWorkflowException.class,
                () -> transactionService.withdraw("withdraw-101", moneyRequest("ACC000101", "10.01")));
    }

    @Test
    public void preventsMoneyMovementForFrozenAccount() {
        Account account = account("ACC000101", "100.00", "INR");
        account.setAccountStatus(AccountStatus.FROZEN);
        when(accountRepository.findByAccountNumberForUpdate("ACC000101")).thenReturn(Optional.of(account));

        expectThrows(TransactionWorkflowException.class,
                () -> transactionService.deposit("deposit-101", moneyRequest("ACC000101", "1.00")));
    }

    @Test
    public void transfersFundsAtomicallyAndCreatesDebitAndCreditEntries() {
        Account source = account("ACC000101", "100.00", "INR");
        Account destination = account("ACC000102", "50.00", "INR");
        when(accountRepository.findByAccountNumberForUpdate("ACC000101")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumberForUpdate("ACC000102")).thenReturn(Optional.of(destination));

        FundTransferResponse response = transactionService.transfer("transfer-101", transferRequest("20.00"));

        assertEquals(response.getStatus(), "BOOKED");
        assertEquals(response.getSourceBalanceAfter(), new BigDecimal("80.00"));
        assertEquals(response.getDestinationBalanceAfter(), new BigDecimal("70.00"));
        assertEquals(source.getBalance(), new BigDecimal("80.00"));
        assertEquals(destination.getBalance(), new BigDecimal("70.00"));
        verify(ledgerTransactionRepository, org.mockito.Mockito.times(2)).save(any(LedgerTransaction.class));
    }

    @Test
    public void preventsCrossCurrencyTransfer() {
        Account source = account("ACC000101", "100.00", "INR");
        Account destination = account("ACC000102", "50.00", "USD");
        when(accountRepository.findByAccountNumberForUpdate("ACC000101")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumberForUpdate("ACC000102")).thenReturn(Optional.of(destination));

        expectThrows(TransactionWorkflowException.class,
                () -> transactionService.transfer("transfer-101", transferRequest("20.00")));
    }

    @Test
    public void returnsTransactionHistoryWithTypeFilter() {
        Account account = account("ACC000101", "100.00", "INR");
        LedgerTransaction transaction = ledgerTransaction(account, LedgerTransactionType.WITHDRAWAL, "90.00");
        when(accountRepository.findByAccountNumber("ACC000101")).thenReturn(Optional.of(account));
        when(ledgerTransactionRepository.findByAccount_IdAndTypeOrderByOccurredAtDesc(
                org.mockito.Mockito.eq(101L), org.mockito.Mockito.eq(LedgerTransactionType.WITHDRAWAL),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(transaction), PageRequest.of(0, 20), 1));

        TransactionHistoryResponse response = transactionService.getTransactionHistory(
                "ACC000101", 0, 20, "withdrawal");

        assertEquals(response.getTotalElements(), 1L);
        assertEquals(response.getTransactions().get(0).getType(), LedgerTransactionType.WITHDRAWAL.name());
    }

    private Account account(String accountNumber, String balance, String currency) {
        Account account = new Account();
        account.setId(Long.valueOf(accountNumber.substring(3)));
        account.setAccountNumber(accountNumber);
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setBalance(new BigDecimal(balance));
        account.setCurrency(currency);
        return account;
    }

    private MoneyTransactionRequest moneyRequest(String accountNumber, String amount) {
        MoneyTransactionRequest request = new MoneyTransactionRequest();
        request.setAccountNumber(accountNumber);
        request.setAmount(new BigDecimal(amount));
        request.setDescription("Cash operation.");
        return request;
    }

    private FundTransferRequest transferRequest(String amount) {
        FundTransferRequest request = new FundTransferRequest();
        request.setSourceAccountNumber("ACC000101");
        request.setDestinationAccountNumber("ACC000102");
        request.setAmount(new BigDecimal(amount));
        request.setDescription("Internal transfer.");
        return request;
    }

    private LedgerTransaction ledgerTransaction(Account account, LedgerTransactionType type, String balanceAfter) {
        LedgerTransaction transaction = new LedgerTransaction();
        transaction.setReference("TXN-TEST");
        transaction.setAccount(account);
        transaction.setType(type);
        transaction.setAmount(new BigDecimal("25.00"));
        transaction.setBalanceAfter(new BigDecimal(balanceAfter));
        transaction.setCurrency(account.getCurrency());
        transaction.setOccurredAt(LocalDateTime.now());
        return transaction;
    }
}
