package com.bank.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bank.api.dto.ActivateCustomerRequest;
import com.bank.api.dto.CustomerRequest;
import com.bank.api.dto.CustomerResponse;
import com.bank.api.exception.InvalidCustomerException;
import com.bank.api.service.CustomerService;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    /**
     * ==========================================================
     * Story-001
     * Enterprise Customer Registration
     * ==========================================================
     */
    @PostMapping("/register")
    public ResponseEntity<CustomerResponse> registerCustomer(
            @RequestBody CustomerRequest request) {

        CustomerResponse response =
                customerService.register(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    /**
     * ==========================================================
     * Story-003A
     * Customer Activation
     * ==========================================================
     */
    @PostMapping("/{customerNumber}/activate")
    public ResponseEntity<CustomerResponse> activateCustomer(
            @PathVariable("customerNumber") String customerNumber,
            @RequestBody ActivateCustomerRequest request) {

        CustomerResponse response =
                customerService.activateCustomer(
                        customerNumber,
                        request);

        return ResponseEntity.ok(response);
    }
    /**
     * ==========================================================
     * Story-002
     * Search Customer by Customer Number
     * ==========================================================
     */
    @GetMapping("/{customerNumber}")
    public ResponseEntity<CustomerResponse> getCustomerByCustomerNumber(
            @PathVariable String customerNumber) {

        customerNumber = customerNumber.trim();

        if (customerNumber.isEmpty()) {
            throw new InvalidCustomerException("Customer Number is mandatory.");
        }

        if (!customerNumber.matches("^CUST\\d{6}$")) {
            throw new InvalidCustomerException("Invalid Customer Number format.");
        }

        CustomerResponse response =
                customerService.getCustomerByCustomerNumber(customerNumber);

        return ResponseEntity.ok(response);
    }
    /**
     * ==========================================================
     * Story-002
     * Search Customer by PAN
     * ==========================================================
     */
    @GetMapping("/search")
    public ResponseEntity<CustomerResponse> getCustomerByPan(
            @RequestParam String pan) {

        CustomerResponse response =
                customerService.getCustomerByPan(pan);

        return ResponseEntity.ok(response);
    }
    /**
     * ==========================================================
     * Story-002
     * Search Customer by Email
     * ==========================================================
     */
    @GetMapping("/search/email")
    public ResponseEntity<CustomerResponse> getCustomerByEmail(
            @RequestParam String email) {

        CustomerResponse response =
                customerService.getCustomerByEmail(email);

        return ResponseEntity.ok(response);
    }
    /**
     * ==========================================================
     * Story-002
     * Search Customer by Phone
     * ==========================================================
     */
    @GetMapping("/search/phone")
    public ResponseEntity<CustomerResponse> getCustomerByPhone(
            @RequestParam String phone) {

        CustomerResponse response =
                customerService.getCustomerByPhone(phone);

        return ResponseEntity.ok(response);
    }
    
    }

