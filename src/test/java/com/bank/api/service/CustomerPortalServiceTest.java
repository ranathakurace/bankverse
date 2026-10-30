package com.bank.api.service;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;
import static org.mockito.Mockito.mock;

import com.bank.api.dto.CustomerOnlineAccessRequest;
import com.bank.api.exception.CustomerPortalException;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.CustomerOnlineAccessRepository;
import com.bank.api.repository.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testng.annotations.Test;

class CustomerPortalServiceTest {

    @Test
    void rejectsNullEnrollmentRequestBeforeAccessingItsFields() {
        CustomerPortalService service = new CustomerPortalService(
                mock(CustomerRepository.class),
                mock(CustomerOnlineAccessRepository.class),
                mock(AccountRepository.class),
                mock(LedgerTransactionService.class),
                mock(PasswordEncoder.class));

        CustomerPortalException exception = expectThrows(CustomerPortalException.class,
                () -> service.enroll((CustomerOnlineAccessRequest) null));

        assertEquals(exception.getMessage(), "Online banking enrollment request is mandatory.");
    }
}
