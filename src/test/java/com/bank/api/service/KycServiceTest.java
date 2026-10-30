package com.bank.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

import com.bank.api.dto.KycAuditResponse;
import com.bank.api.dto.KycResponse;
import com.bank.api.dto.KycReviewRequest;
import com.bank.api.dto.KycReviewStartRequest;
import com.bank.api.dto.KycSubmissionRequest;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class KycServiceTest {

    private CustomerRepository customerRepository;
    private KycCaseRepository kycCaseRepository;
    private KycAuditEventRepository kycAuditEventRepository;
    private KycService kycService;

    @BeforeMethod
    public void setUp() {
        customerRepository = mock(CustomerRepository.class);
        kycCaseRepository = mock(KycCaseRepository.class);
        kycAuditEventRepository = mock(KycAuditEventRepository.class);
        kycService = new KycService(customerRepository, kycCaseRepository, kycAuditEventRepository);
    }

    @Test
    public void submitsPendingKycCustomerAndWritesAuditEvent() {
        Customer customer = customer(KycStatus.PENDING);
        when(customerRepository.findByCustomerNumber("CUST000003")).thenReturn(Optional.of(customer));
        when(kycCaseRepository.save(any(KycCase.class))).thenAnswer(invocation -> {
            KycCase kycCase = invocation.getArgument(0);
            kycCase.setId(77L);
            return kycCase;
        });

        KycResponse response = kycService.submitKyc("cust000003", submissionRequest());

        assertEquals(response.getKycStatus(), KycStatus.SUBMITTED.name());
        assertEquals(response.getKycCaseId(), Long.valueOf(77L));
        assertEquals(customer.getKycStatus(), KycStatus.SUBMITTED.name());
        verify(customerRepository).save(customer);
        verify(kycAuditEventRepository).save(any(KycAuditEvent.class));
    }

    @Test
    public void preventsKycSubmissionForActiveCustomer() {
        Customer customer = customer(KycStatus.PENDING);
        customer.setCustomerStatus(CustomerStatus.ACTIVE);
        when(customerRepository.findByCustomerNumber("CUST000003")).thenReturn(Optional.of(customer));

        expectThrows(KycWorkflowException.class,
                () -> kycService.submitKyc("CUST000003", submissionRequest()));
    }

    @Test
    public void startsReviewForSubmittedKyc() {
        Customer customer = customer(KycStatus.SUBMITTED);
        KycCase kycCase = kycCase(customer, KycStatus.SUBMITTED);
        when(customerRepository.findByCustomerNumber("CUST000003")).thenReturn(Optional.of(customer));
        when(kycCaseRepository.findTopByCustomer_IdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(kycCase));
        when(kycCaseRepository.save(any(KycCase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        KycReviewStartRequest request = new KycReviewStartRequest();
        request.setReviewedBy("OPS-2001");
        request.setRemarks("Review accepted.");

        KycResponse response = kycService.startKycReview("CUST000003", request);

        assertEquals(response.getKycStatus(), KycStatus.UNDER_REVIEW.name());
        assertEquals(kycCase.getStatus(), KycStatus.UNDER_REVIEW);
        verify(kycAuditEventRepository).save(any(KycAuditEvent.class));
    }

    @Test
    public void approvesKycUnderReview() {
        Customer customer = customer(KycStatus.UNDER_REVIEW);
        KycCase kycCase = kycCase(customer, KycStatus.UNDER_REVIEW);
        when(customerRepository.findByCustomerNumber("CUST000003")).thenReturn(Optional.of(customer));
        when(kycCaseRepository.findTopByCustomer_IdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(kycCase));
        when(kycCaseRepository.save(any(KycCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KycResponse response = kycService.reviewKyc("CUST000003",
                reviewRequest(KycDecision.APPROVE, "All identity checks passed."));

        assertEquals(response.getKycStatus(), KycStatus.COMPLETED.name());
        assertEquals(customer.getKycStatus(), KycStatus.COMPLETED.name());
        assertEquals(kycCase.getStatus(), KycStatus.COMPLETED);
    }

    @Test
    public void rejectsKycUnderReviewWhenReasonIsSupplied() {
        Customer customer = customer(KycStatus.UNDER_REVIEW);
        KycCase kycCase = kycCase(customer, KycStatus.UNDER_REVIEW);
        when(customerRepository.findByCustomerNumber("CUST000003")).thenReturn(Optional.of(customer));
        when(kycCaseRepository.findTopByCustomer_IdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(kycCase));
        when(kycCaseRepository.save(any(KycCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KycResponse response = kycService.reviewKyc("CUST000003",
                reviewRequest(KycDecision.REJECT, "Document image is unreadable."));

        assertEquals(response.getKycStatus(), KycStatus.REJECTED.name());
        assertEquals(customer.getKycStatus(), KycStatus.REJECTED.name());
        assertEquals(kycCase.getStatus(), KycStatus.REJECTED);
    }

    @Test
    public void preventsKycRejectionWithoutReason() {
        Customer customer = customer(KycStatus.UNDER_REVIEW);
        when(customerRepository.findByCustomerNumber("CUST000003")).thenReturn(Optional.of(customer));

        expectThrows(InvalidKycException.class,
                () -> kycService.reviewKyc("CUST000003", reviewRequest(KycDecision.REJECT, " ")));
    }

    @Test
    public void returnsChronologicalKycAuditHistory() {
        Customer customer = customer(KycStatus.COMPLETED);
        KycAuditEvent submitted = auditEvent(KycStatus.SUBMITTED, "KYC_SUBMITTED", "OPS-1001");
        KycAuditEvent approved = auditEvent(KycStatus.COMPLETED, "KYC_APPROVED", "OPS-2001");
        when(customerRepository.findByCustomerNumber("CUST000003")).thenReturn(Optional.of(customer));
        when(kycAuditEventRepository.findByCustomer_IdOrderByOccurredAtAsc(1L))
                .thenReturn(Arrays.asList(submitted, approved));

        List<KycAuditResponse> history = kycService.getKycHistory("CUST000003");

        assertEquals(history.size(), 2);
        assertEquals(history.get(0).getAction(), "KYC_SUBMITTED");
        assertEquals(history.get(1).getStatus(), KycStatus.COMPLETED.name());
    }

    private Customer customer(KycStatus kycStatus) {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCustomerNumber("CUST000003");
        customer.setCustomerStatus(CustomerStatus.PENDING_KYC);
        customer.setKycStatus(kycStatus.name());
        return customer;
    }

    private KycCase kycCase(Customer customer, KycStatus status) {
        KycCase kycCase = new KycCase();
        kycCase.setId(77L);
        kycCase.setCustomer(customer);
        kycCase.setStatus(status);
        return kycCase;
    }

    private KycSubmissionRequest submissionRequest() {
        KycSubmissionRequest request = new KycSubmissionRequest();
        request.setDocumentReference("KYC-DOC-000003");
        request.setSubmittedBy("OPS-1001");
        request.setRemarks("Identity documents received.");
        return request;
    }

    private KycReviewRequest reviewRequest(KycDecision decision, String remarks) {
        KycReviewRequest request = new KycReviewRequest();
        request.setDecision(decision);
        request.setReviewedBy("OPS-2001");
        request.setRemarks(remarks);
        return request;
    }

    private KycAuditEvent auditEvent(KycStatus status, String action, String performedBy) {
        KycAuditEvent event = new KycAuditEvent();
        event.setStatus(status);
        event.setAction(action);
        event.setPerformedBy(performedBy);
        event.setOccurredAt(LocalDateTime.now());
        return event;
    }
}
