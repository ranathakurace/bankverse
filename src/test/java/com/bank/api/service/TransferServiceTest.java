package com.bank.api.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.expectThrows;

import com.bank.api.dto.TransferRequest;
import com.bank.api.exception.TransferFailedException;
import com.bank.api.model.Account;
import com.bank.api.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;

class TransferServiceTest {

    @Test
    void rejectsSameAccountEvenWhenLongIdentifiersAreDifferentObjects() {
        AccountRepository repository = mock(AccountRepository.class);
        Account source = account(Long.valueOf(1000L));
        Account destination = account(Long.valueOf(1000L));
        AtomicInteger lookupCount = new AtomicInteger();
        when(repository.findById(1000L)).thenAnswer(invocation -> Optional.of(
                lookupCount.getAndIncrement() == 0 ? source : destination));

        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1000);
        request.setToAccountId(1000);
        request.setAmount(10.00);

        expectThrows(TransferFailedException.class,
                () -> new TransferService(repository).transfer("quality-" + UUID.randomUUID(), request));
    }

    private Account account(Long id) {
        Account account = new Account();
        account.setId(id);
        account.setBalance(BigDecimal.valueOf(100));
        return account;
    }
}
