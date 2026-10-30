package com.bank.api.service;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.bank.api.dto.ActivateCustomerRequest;
import com.bank.api.dto.CustomerResponse;
import com.bank.api.exception.CustomerNotFoundException;
import com.bank.api.exception.InvalidCustomerException;
import com.bank.api.model.Customer;
import com.bank.api.model.CustomerStatus;
import com.bank.api.repository.CustomerRepository;

class CustomerServiceTest {

    private static final String CUSTOMER_NUMBER = "CUST000001";

    private CustomerRepository customerRepository;

    private CustomerService customerService;

    @BeforeMethod
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        customerService = new CustomerService(customerRepository);
    }

    @Test
    void activatesPendingKycCustomerWhenKycIsCompleted() {
        Customer customer = customer(CustomerStatus.PENDING_KYC, "COMPLETED");
        when(customerRepository.findByCustomerNumber(CUSTOMER_NUMBER))
                .thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        CustomerResponse response = customerService.activateCustomer(
                " cust000001 ", new ActivateCustomerRequest());

        assertEquals("ACTIVE", response.getCustomerStatus());
        assertEquals("Customer activated successfully.", response.getMessage());
        assertEquals(CustomerStatus.ACTIVE, customer.getCustomerStatus());
        verify(customerRepository).save(customer);
    }

    @Test
    void rejectsAnAlreadyActiveCustomer() {
        Customer customer = customer(CustomerStatus.ACTIVE, "COMPLETED");
        when(customerRepository.findByCustomerNumber(CUSTOMER_NUMBER))
                .thenReturn(Optional.of(customer));

        InvalidCustomerException exception = expectThrows(InvalidCustomerException.class,
                () -> customerService.activateCustomer(CUSTOMER_NUMBER, new ActivateCustomerRequest()));

        assertEquals("Customer is already active.", exception.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void rejectsCustomerThatIsNotPendingKyc() {
        Customer customer = customer(CustomerStatus.BLOCKED, "COMPLETED");
        when(customerRepository.findByCustomerNumber(CUSTOMER_NUMBER))
                .thenReturn(Optional.of(customer));

        InvalidCustomerException exception = expectThrows(InvalidCustomerException.class,
                () -> customerService.activateCustomer(CUSTOMER_NUMBER, new ActivateCustomerRequest()));

        assertEquals("Customer must be in PENDING_KYC status before activation.", exception.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void rejectsCustomerWhoseKycIsNotCompleted() {
        Customer customer = customer(CustomerStatus.PENDING_KYC, "PENDING");
        when(customerRepository.findByCustomerNumber(CUSTOMER_NUMBER))
                .thenReturn(Optional.of(customer));

        InvalidCustomerException exception = expectThrows(InvalidCustomerException.class,
                () -> customerService.activateCustomer(CUSTOMER_NUMBER, new ActivateCustomerRequest()));

        assertEquals("Customer KYC must be COMPLETED before activation.", exception.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void rejectsAnInvalidCustomerNumberBeforeDatabaseLookup() {
        InvalidCustomerException exception = expectThrows(InvalidCustomerException.class,
                () -> customerService.activateCustomer("INVALID", new ActivateCustomerRequest()));

        assertEquals("Invalid Customer Number format.", exception.getMessage());
        verify(customerRepository, never()).findByCustomerNumber(any(String.class));
    }

    @Test
    void reportsCustomerNotFound() {
        when(customerRepository.findByCustomerNumber(CUSTOMER_NUMBER))
                .thenReturn(Optional.empty());

        expectThrows(CustomerNotFoundException.class,
                () -> customerService.activateCustomer(CUSTOMER_NUMBER, new ActivateCustomerRequest()));

        verify(customerRepository, never()).save(any(Customer.class));
    }

    private Customer customer(CustomerStatus customerStatus, String kycStatus) {
        Customer customer = new Customer();
        customer.setCustomerNumber(CUSTOMER_NUMBER);
        customer.setFullName("Test Customer");
        customer.setCustomerStatus(customerStatus);
        customer.setKycStatus(kycStatus);
        customer.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        return customer;
    }
}
