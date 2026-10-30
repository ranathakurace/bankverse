package com.bank.api.service;

import com.bank.api.dto.KycAuditResponse;
import com.bank.api.dto.KycResponse;
import com.bank.api.dto.KycReviewRequest;
import com.bank.api.dto.KycReviewStartRequest;
import com.bank.api.dto.KycSubmissionRequest;
import com.bank.api.exception.CustomerNotFoundException;
import com.bank.api.exception.InvalidKycException;
import com.bank.api.exception.KycWorkflowException;
import com.bank.api.model.Customer;
import com.bank.api.model.CustomerStatus;
import com.bank.api.model.KycAuditEvent;
import com.bank.api.model.KycCase;
import com.bank.api.model.KycDecision;
import com.bank.api.model.KycStatus;
import com.bank.api.repository.CustomerRepository;
import com.bank.api.repository.KycAuditEventRepository;
import com.bank.api.repository.KycCaseRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KycService {

    private static final String CUSTOMER_NUMBER_REGEX = "^CUST\\d{6}$";
    private static final int ACTOR_MAX_LENGTH = 100;
    private static final int DOCUMENT_REFERENCE_MAX_LENGTH = 100;
    private static final int REMARKS_MAX_LENGTH = 500;

    private final CustomerRepository customerRepository;
    private final KycCaseRepository kycCaseRepository;
    private final KycAuditEventRepository kycAuditEventRepository;

    public KycService(CustomerRepository customerRepository, KycCaseRepository kycCaseRepository,
            KycAuditEventRepository kycAuditEventRepository) {
        this.customerRepository = customerRepository;
        this.kycCaseRepository = kycCaseRepository;
        this.kycAuditEventRepository = kycAuditEventRepository;
    }

    @Transactional
    public KycResponse submitKyc(String customerNumber, KycSubmissionRequest request) {
        Customer customer = findCustomer(customerNumber);
        validateSubmission(customer, request);
        LocalDateTime now = LocalDateTime.now();
        KycCase kycCase = new KycCase();
        kycCase.setCustomer(customer);
        kycCase.setStatus(KycStatus.SUBMITTED);
        kycCase.setDocumentReference(trim(request.getDocumentReference()));
        kycCase.setSubmittedBy(trim(request.getSubmittedBy()));
        kycCase.setSubmissionRemarks(trimOptionalRemarks(request.getRemarks()));
        kycCase.setSubmittedAt(now);
        kycCase.setCreatedAt(now);
        customer.setKycStatus(KycStatus.SUBMITTED.name());
        customer.setUpdatedAt(now);
        customerRepository.save(customer);
        KycCase savedKycCase = kycCaseRepository.save(kycCase);
        recordAudit(customer, KycStatus.SUBMITTED, "KYC_SUBMITTED", kycCase.getSubmittedBy(),
                kycCase.getSubmissionRemarks(), now);
        return response(savedKycCase, customer, "KYC submitted successfully.", now);
    }

    @Transactional
    public KycResponse startKycReview(String customerNumber, KycReviewStartRequest request) {
        Customer customer = findCustomer(customerNumber);
        requireKycStatus(customer, KycStatus.SUBMITTED,
                "KYC must be in SUBMITTED status before a review can start.");
        validateReviewStartRequest(request);
        KycCase kycCase = findLatestKycCase(customer);
        LocalDateTime now = LocalDateTime.now();
        String reviewer = trim(request.getReviewedBy());
        String remarks = trimOptionalRemarks(request.getRemarks());
        customer.setKycStatus(KycStatus.UNDER_REVIEW.name());
        customer.setUpdatedAt(now);
        customerRepository.save(customer);
        kycCase.setStatus(KycStatus.UNDER_REVIEW);
        kycCase.setReviewedBy(reviewer);
        kycCase.setReviewRemarks(remarks);
        kycCase.setReviewStartedAt(now);
        KycCase savedKycCase = kycCaseRepository.save(kycCase);
        recordAudit(customer, KycStatus.UNDER_REVIEW, "KYC_REVIEW_STARTED", reviewer, remarks, now);
        return response(savedKycCase, customer, "KYC review started successfully.", now);
    }

    @Transactional
    public KycResponse reviewKyc(String customerNumber, KycReviewRequest request) {
        Customer customer = findCustomer(customerNumber);
        requireKycStatus(customer, KycStatus.UNDER_REVIEW,
                "KYC must be in UNDER_REVIEW status before a decision is recorded.");
        validateReviewRequest(request);
        KycCase kycCase = findLatestKycCase(customer);
        LocalDateTime now = LocalDateTime.now();
        KycStatus outcome = request.getDecision() == KycDecision.APPROVE
                ? KycStatus.COMPLETED : KycStatus.REJECTED;
        String reviewer = trim(request.getReviewedBy());
        String remarks = trimOptionalRemarks(request.getRemarks());
        customer.setKycStatus(outcome.name());
        customer.setUpdatedAt(now);
        customerRepository.save(customer);
        kycCase.setStatus(outcome);
        kycCase.setReviewedBy(reviewer);
        kycCase.setReviewRemarks(remarks);
        kycCase.setReviewedAt(now);
        KycCase savedKycCase = kycCaseRepository.save(kycCase);
        String action = outcome == KycStatus.COMPLETED ? "KYC_APPROVED" : "KYC_REJECTED";
        String message = outcome == KycStatus.COMPLETED
                ? "KYC approved successfully." : "KYC rejected successfully.";
        recordAudit(customer, outcome, action, reviewer, remarks, now);
        return response(savedKycCase, customer, message, now);
    }

    @Transactional(readOnly = true)
    public List<KycAuditResponse> getKycHistory(String customerNumber) {
        Customer customer = findCustomer(customerNumber);
        return kycAuditEventRepository.findByCustomer_IdOrderByOccurredAtAsc(customer.getId()).stream()
                .map(this::auditResponse)
                .collect(Collectors.toList());
    }

    private Customer findCustomer(String customerNumber) {
        String normalizedCustomerNumber = normalizeCustomerNumber(customerNumber);
        return customerRepository.findByCustomerNumber(normalizedCustomerNumber)
                .orElseThrow(() -> new CustomerNotFoundException("Customer Number", normalizedCustomerNumber));
    }

    private KycCase findLatestKycCase(Customer customer) {
        return kycCaseRepository.findTopByCustomer_IdOrderByCreatedAtDesc(customer.getId())
                .orElseThrow(() -> new KycWorkflowException("No KYC case exists for this customer."));
    }

    private void validateSubmission(Customer customer, KycSubmissionRequest request) {
        if (customer.getCustomerStatus() != CustomerStatus.PENDING_KYC) {
            throw new KycWorkflowException("Only customers in PENDING_KYC status can submit KYC.");
        }
        KycStatus currentStatus = currentKycStatus(customer);
        if (currentStatus != KycStatus.PENDING && currentStatus != KycStatus.REJECTED) {
            throw new KycWorkflowException("KYC submission is not allowed while the KYC status is "
                    + currentStatus.name() + ".");
        }
        if (request == null) {
            throw new InvalidKycException("KYC submission request is mandatory.");
        }
        validateDocumentReference(request.getDocumentReference());
        validateActor(request.getSubmittedBy(), "Submitted by");
        validateRemarks(request.getRemarks());
    }

    private void validateReviewStartRequest(KycReviewStartRequest request) {
        if (request == null) {
            throw new InvalidKycException("KYC review start request is mandatory.");
        }
        validateActor(request.getReviewedBy(), "Reviewed by");
        validateRemarks(request.getRemarks());
    }

    private void validateReviewRequest(KycReviewRequest request) {
        if (request == null) {
            throw new InvalidKycException("KYC review request is mandatory.");
        }
        if (request.getDecision() == null) {
            throw new InvalidKycException("KYC review decision is mandatory.");
        }
        validateActor(request.getReviewedBy(), "Reviewed by");
        validateRemarks(request.getRemarks());
        if (request.getDecision() == KycDecision.REJECT && isBlank(request.getRemarks())) {
            throw new InvalidKycException("Remarks are mandatory when KYC is rejected.");
        }
    }

    private void requireKycStatus(Customer customer, KycStatus expectedStatus, String message) {
        if (currentKycStatus(customer) != expectedStatus) {
            throw new KycWorkflowException(message);
        }
    }

    private KycStatus currentKycStatus(Customer customer) {
        if (isBlank(customer.getKycStatus())) {
            return KycStatus.PENDING;
        }
        try {
            return KycStatus.valueOf(customer.getKycStatus().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new KycWorkflowException("Customer has an unsupported KYC status.");
        }
    }

    private String normalizeCustomerNumber(String customerNumber) {
        if (isBlank(customerNumber)) {
            throw new InvalidKycException("Customer Number is mandatory.");
        }
        String normalizedCustomerNumber = customerNumber.trim().toUpperCase(Locale.ROOT);
        if (!normalizedCustomerNumber.matches(CUSTOMER_NUMBER_REGEX)) {
            throw new InvalidKycException("Invalid Customer Number format.");
        }
        return normalizedCustomerNumber;
    }

    private void validateDocumentReference(String documentReference) {
        if (isBlank(documentReference) || documentReference.trim().length() > DOCUMENT_REFERENCE_MAX_LENGTH) {
            throw new InvalidKycException("Document reference must contain between 1 and "
                    + DOCUMENT_REFERENCE_MAX_LENGTH + " characters.");
        }
    }

    private void validateActor(String actor, String fieldName) {
        if (isBlank(actor) || actor.trim().length() > ACTOR_MAX_LENGTH) {
            throw new InvalidKycException(fieldName + " must contain between 1 and "
                    + ACTOR_MAX_LENGTH + " characters.");
        }
    }

    private void validateRemarks(String remarks) {
        if (remarks != null && remarks.trim().length() > REMARKS_MAX_LENGTH) {
            throw new InvalidKycException("Remarks cannot exceed " + REMARKS_MAX_LENGTH + " characters.");
        }
    }

    private void recordAudit(Customer customer, KycStatus status, String action, String performedBy,
            String remarks, LocalDateTime occurredAt) {
        KycAuditEvent auditEvent = new KycAuditEvent();
        auditEvent.setCustomer(customer);
        auditEvent.setStatus(status);
        auditEvent.setAction(action);
        auditEvent.setPerformedBy(performedBy);
        auditEvent.setRemarks(remarks);
        auditEvent.setOccurredAt(occurredAt);
        kycAuditEventRepository.save(auditEvent);
    }

    private KycResponse response(KycCase kycCase, Customer customer, String message, LocalDateTime processedAt) {
        KycResponse response = new KycResponse();
        response.setKycCaseId(kycCase.getId());
        response.setCustomerNumber(customer.getCustomerNumber());
        response.setKycStatus(kycCase.getStatus().name());
        response.setMessage(message);
        response.setProcessedAt(processedAt);
        return response;
    }

    private KycAuditResponse auditResponse(KycAuditEvent event) {
        KycAuditResponse response = new KycAuditResponse();
        response.setAction(event.getAction());
        response.setStatus(event.getStatus().name());
        response.setPerformedBy(event.getPerformedBy());
        response.setRemarks(event.getRemarks());
        response.setOccurredAt(event.getOccurredAt());
        return response;
    }

    private String trim(String value) {
        return value.trim();
    }

    private String trimOptionalRemarks(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
