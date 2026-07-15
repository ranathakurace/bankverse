package com.bank.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.regex.Pattern;
import com.bank.api.util.ValidationUtil;
import com.bank.api.dto.ActivateCustomerRequest;
import com.bank.api.dto.CustomerRequest;
import com.bank.api.dto.CustomerResponse;
import com.bank.api.exception.CustomerAlreadyExistsException;
import com.bank.api.exception.CustomerNotFoundException;
import com.bank.api.exception.InvalidCustomerException;
import com.bank.api.model.Customer;
import com.bank.api.model.CustomerStatus;
import com.bank.api.repository.CustomerRepository;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    private Customer saveCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomer(Long id) {
        return customerRepository.findById(id).orElse(null);
    }

    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }
    public CustomerResponse register(CustomerRequest request) {

    	/*
    	 * =========================================================
    	 * Step 1 : Validate Request
    	 * =========================================================
    	 */

    	if (request == null) {
    	    throw new InvalidCustomerException("Customer request cannot be null.");
    	}

    	if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
    	    throw new InvalidCustomerException("Full Name is mandatory.");
    	}

    	if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
    		throw new InvalidCustomerException("Email is mandatory.");
    	}

    	if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
    	    throw new InvalidCustomerException("Phone Number is mandatory.");
    	}

    	if (request.getPan() == null || request.getPan().trim().isEmpty()) {
    	    throw new InvalidCustomerException("PAN Number is mandatory.");
    	}

    	if (request.getAadhaar() == null || request.getAadhaar().trim().isEmpty()) {
    	    throw new InvalidCustomerException("Aadhaar Number is mandatory.");
    	}

    	if (request.getDob() == null) {
    	    throw new InvalidCustomerException("Date of Birth is mandatory.");
    	}

    	if (request.getDob().isAfter(LocalDate.now().minusYears(18))) {
    	    throw new InvalidCustomerException("Customer must be at least 18 years old.");
    	}
    	/*
    	 * ==========================================================
    	 * Step 1.0 : Validate Field Length
    	 * ==========================================================
    	 */

    	// Full Name
    	if (request.getFullName().trim().length() > 100) {
    	    throw new InvalidCustomerException(
    	            "Full Name cannot exceed 100 characters.");
    	}

    	// Email
    	if (request.getEmail().trim().length() > 255) {
    	    throw new InvalidCustomerException(
    	            "Email cannot exceed 255 characters.");
    	}
    	/*
    	 * ==========================================================
    	 * Step 1.1 : Validate Customer Data Format
    	 * ==========================================================
    	 */

    	// Full Name
    	if (!Pattern.matches(ValidationUtil.NAME_REGEX,
    	        request.getFullName().trim())) {

    	    throw new InvalidCustomerException(
    	            "Full Name should contain only alphabets and spaces.");
    	}

    	// Email
    	if (!Pattern.matches(ValidationUtil.EMAIL_REGEX,
    	        request.getEmail().trim())) {

    	    throw new InvalidCustomerException(
    	            "Invalid Email format.");
    	}

    	// Phone
    	if (!Pattern.matches(ValidationUtil.PHONE_REGEX,
    	        request.getPhone().trim())) {

    	    throw new InvalidCustomerException(
    	            "Invalid Phone Number.");
    	}

    	// PAN
    	if (!Pattern.matches(ValidationUtil.PAN_REGEX,
    	        request.getPan().trim().toUpperCase())) {

    	    throw new InvalidCustomerException(
    	            "Invalid PAN Number.");
    	}

    	// Aadhaar
    	if (!Pattern.matches(ValidationUtil.AADHAAR_REGEX,
    	        request.getAadhaar().trim())) {

    	    throw new InvalidCustomerException(
    	            "Invalid Aadhaar Number.");
    	}

    	/*
    	 * ==========================================================
    	 * Step 2 : Check Duplicate Customer Information
    	 * ==========================================================
    	 *
    	 * Enterprise Validation
    	 * ---------------------
    	 * PAN
    	 * Aadhaar
    	 * Email
    	 * Phone
    	 *
    	 */

    	// PAN
    	if (customerRepository.existsByPan(request.getPan().trim().toUpperCase())) {
    	    throw new CustomerAlreadyExistsException("PAN already registered.");
    	}

    	// Aadhaar
    	if (customerRepository.existsByAadhaar(request.getAadhaar().trim())) {
    	    throw new CustomerAlreadyExistsException("Aadhaar already registered.");
    	}

    	// Email
    	if (customerRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
    	    throw new CustomerAlreadyExistsException("Email already registered.");
    	}

    	// Phone
    	if (customerRepository.existsByPhone(request.getPhone().trim())) {
    	    throw new CustomerAlreadyExistsException("Phone Number already registered.");
    	}

    	/*
    	 * ==========================================================
    	 * Step 3 : Generate Customer Number
    	 * ==========================================================
    	 */
    	
    	Long sequenceNumber = customerRepository.getNextCustomerSequence();

    	String customerNumber = String.format("CUST%06d", sequenceNumber);

        /*
         * =========================================================
         * Step 4 : Create Customer Entity
         * =========================================================
         */
    	Customer customer = new Customer();

    	customer.setCustomerNumber(customerNumber);

    	customer.setFullName(request.getFullName().trim());

    	customer.setEmail(request.getEmail().trim().toLowerCase());

    	customer.setPhone(request.getPhone().trim());

    	customer.setPan(request.getPan().trim().toUpperCase());

    	customer.setAadhaar(request.getAadhaar().trim());

    	customer.setDob(request.getDob());

    	/*
    	 * ===============================================================
    	 * Step 4.1 : Initialize Enterprise Defaults
    	 * ===============================================================
    	 */

    	customer.setCustomerStatus(CustomerStatus.PENDING_KYC);

    	customer.setKycStatus("PENDING");

    	customer.setRiskScore(0);

    	customer.setCreatedAt(LocalDateTime.now());

    	customer.setUpdatedAt(LocalDateTime.now());
    	
        /*
         * =========================================================
         * Step 5 : Save Customer
         * =========================================================
         */
    	Customer savedCustomer = saveCustomer(customer);	

        /*
         * =========================================================
         * Step 6 : Build Response
         * =========================================================
         */

    	CustomerResponse response = mapToCustomerResponse(savedCustomer);
    	response.setMessage("Customer registered successfully.");
    	return response;
    }
    /**
     * ==========================================================
     * Customer Activation Workflow
     * ==========================================================
     */
    public CustomerResponse activateCustomer(
            String customerNumber,
            ActivateCustomerRequest request) {

        Customer customer = findCustomerByNumber(customerNumber);
        validateCustomerForActivation(customer);
        activateCustomerStatus(customer);
        Customer savedCustomer = saveCustomer(customer);
        createAuditLog(savedCustomer, request.getComments());
        sendActivationNotification(savedCustomer);
        return buildActivationResponse(savedCustomer);
    }
    /**
     * ==========================================================
     * Retrieve Customer by Customer Number
     * ==========================================================
     *
     * Business Purpose:
     * Retrieve the customer from the database before performing
     * any banking business operation.
     *
     * Throws:
     * CustomerNotFoundException if customer does not exist.
     *
     * Used By:
     * Customer Activation
     * Future Account Opening
     * Future Fund Transfer
     * Future Loan Processing
     * ==========================================================
     */
    private Customer findCustomerByNumber(String customerNumber) {

        return customerRepository
                .findByCustomerNumber(customerNumber)
                .orElseThrow(() ->
                        new CustomerNotFoundException(
                                "Customer Number",
                                customerNumber));
    }
    public CustomerResponse getCustomerByCustomerNumber(String customerNumber) {

        Customer customer = customerRepository
                .findByCustomerNumber(customerNumber)
                .orElseThrow(() ->
                new CustomerNotFoundException(
                        "customer number",
                        customerNumber));

        return mapToCustomerResponse(customer);
    }
    /**
     * ==========================================================
     * Validate Customer for Activation
     * ==========================================================
     *
     * Business Purpose:
     * Ensure the customer satisfies all business rules before
     * activation is allowed.
     *
     * Current Version 1 Rules:
     * - Customer must be in PENDING_KYC status
     * - KYC must be COMPLETED
     *
     * Future Enhancements:
     * - AI Compliance Advisor
     * - AML Validation
     * - Fraud Detection
     * - Manager Approval
     * ==========================================================
     */
    private void validateCustomerForActivation(Customer customer) {

        // Implementation will be added next.
    }
    /**
     * ==========================================================
     * Activate Customer
     * ==========================================================
     *
     * Business Purpose:
     * Update the customer status from PENDING_KYC to ACTIVE
     * after all banking business rules have been validated.
     *
     * Business Rules:
     * - Customer validation must already be completed.
     * - Status changes only from PENDING_KYC to ACTIVE.
     *
     * Parameters:
     * customer - Customer entity to activate.
     *
     * Returns:
     * None
     *
     * Future Enhancements:
     * - Activation Date
     * - Activated By Employee
     * - AI Recommendation Score
     * ==========================================================
     */
    private void activateCustomerStatus(Customer customer) {

        customer.setCustomerStatus(CustomerStatus.ACTIVE);

        customer.setUpdatedAt(LocalDateTime.now());

    }
    /**
     * ==========================================================
     * Build Customer Activation Response
     * ==========================================================
     *
     * Business Purpose:
     * Build the API response returned to the bank employee after
     * successful customer activation.
     *
     * Business Rules:
     * - Customer Status must be ACTIVE.
     * - Customer Number must be returned.
     * - Success message must be included.
     *
     * Parameters:
     * customer - Activated customer.
     *
     * Returns:
     * CustomerResponse
     *
     * Version 1 Scope:
     * - Customer Number
     * - Customer Status
     * - Success Message
     *
     * Version 2 Scope:
     * - Activation Date
     * - Activated By
     * - AI Recommendation
     * ==========================================================
     */
    private CustomerResponse buildActivationResponse(Customer customer) {

        CustomerResponse response = mapToCustomerResponse(customer);

        response.setMessage("Customer activated successfully.");

        return response;
    }
    /**
     * ==========================================================
     * Create Audit Log
     * ==========================================================
     *
     * Version 1
     * Placeholder
     *
     * Future Story:
     * BV-00XX Audit Service
     * ==========================================================
     */
    private void createAuditLog(Customer customer,
                                String comments) {

        // Placeholder
    }
    /**
     * Send customer activation notification.
     *
     * Reference:
     * BV-003A Customer Activation
     */
    private void sendActivationNotification(Customer customer) {

        // Version 1 Placeholder
        // Future Story: Notification Service
    }
    /**
     * ==========================================================
     * Search Customer by PAN
     * ==========================================================
     */
    public CustomerResponse getCustomerByPan(String pan) {

        // Normalize Input
        String normalizedPan = pan.trim().toUpperCase();

        // Validate PAN Format
        if (!normalizedPan.matches(ValidationUtil.PAN_REGEX)) {
            throw new InvalidCustomerException("Invalid PAN Number.");
        }

        // Search Customer
        Customer customer = customerRepository
                .findByPan(normalizedPan)
                .orElseThrow(() ->
                new CustomerNotFoundException(
                        "PAN",
                        normalizedPan));

        // Map Entity to DTO
        return mapToCustomerResponse(customer);
    }
    /**
     * ==========================================================
     * Search Customer by Email
     * ==========================================================
     */
    public CustomerResponse getCustomerByEmail(String email) {

        // Normalize Input
        String normalizedEmail = email.trim().toLowerCase();

        // Validate Email
        if (!normalizedEmail.matches(ValidationUtil.EMAIL_REGEX)) {
            throw new InvalidCustomerException("Invalid Email Address.");
        }

        // Search Customer
        Customer customer = customerRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() ->
                new CustomerNotFoundException(
                        "Email",
                        normalizedEmail));

        // Map Entity to DTO
        return mapToCustomerResponse(customer);
    }
    /**
     * ==========================================================
     * Search Customer by Phone Number
     * ==========================================================
     */
    public CustomerResponse getCustomerByPhone(String phone) {

        // Normalize
    	String normalizedPhoneNumber = phone.trim();
        // Validate
    	if (!normalizedPhoneNumber.matches(ValidationUtil.PHONE_REGEX)) {
            throw new InvalidCustomerException("Invalid Phone Number.");
        }

        // Search
    	Customer customer = customerRepository
                .findByPhone(normalizedPhoneNumber)
                .orElseThrow(() ->
                new CustomerNotFoundException(
                        "Phone",
                        normalizedPhoneNumber));
        

        return mapToCustomerResponse(customer);

    }
    /**
     * ==========================================================
     * Map Customer Entity to Response DTO
     * ==========================================================
     */
    private CustomerResponse mapToCustomerResponse(Customer customer) {

        CustomerResponse response = new CustomerResponse();

        response.setCustomerNumber(customer.getCustomerNumber());
        response.setFullName(customer.getFullName());
        response.setCustomerStatus(customer.getCustomerStatus().name());
        response.setKycStatus(customer.getKycStatus());

        return response;
    }
   
}