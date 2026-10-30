package com.bank.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

import com.bank.api.dto.ControlCaseResolutionRequest;
import com.bank.api.dto.ControlCaseResponse;
import com.bank.api.dto.ControlNotificationResponse;
import com.bank.api.dto.RiskAssessmentRequest;
import com.bank.api.dto.RiskAssessmentResponse;
import com.bank.api.exception.ControlWorkflowException;
import com.bank.api.model.Account;
import com.bank.api.model.AccountStatus;
import com.bank.api.model.ControlCase;
import com.bank.api.model.ControlCaseStatus;
import com.bank.api.model.ControlNotification;
import com.bank.api.model.NotificationChannel;
import com.bank.api.model.NotificationStatus;
import com.bank.api.model.RiskAssessment;
import com.bank.api.model.RiskDecision;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.ControlCaseRepository;
import com.bank.api.repository.ControlNotificationRepository;
import com.bank.api.repository.RiskAssessmentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EnterpriseControlServiceTest {
    private AccountRepository accountRepository; private RiskAssessmentRepository riskAssessmentRepository; private ControlCaseRepository controlCaseRepository; private ControlNotificationRepository controlNotificationRepository; private EnterpriseControlService controlService;
    @BeforeMethod public void setUp() { accountRepository = mock(AccountRepository.class); riskAssessmentRepository = mock(RiskAssessmentRepository.class); controlCaseRepository = mock(ControlCaseRepository.class); controlNotificationRepository = mock(ControlNotificationRepository.class); controlService = new EnterpriseControlService(accountRepository, riskAssessmentRepository, controlCaseRepository, controlNotificationRepository); when(riskAssessmentRepository.save(any(RiskAssessment.class))).thenAnswer(i -> i.getArgument(0)); when(controlCaseRepository.save(any(ControlCase.class))).thenAnswer(i -> i.getArgument(0)); when(controlNotificationRepository.save(any(ControlNotification.class))).thenAnswer(i -> i.getArgument(0)); }
    @Test public void allowsLowValueRiskAssessmentWithoutOpeningControlCase() {
        when(accountRepository.findByAccountNumber("ACC000101")).thenReturn(Optional.of(account(AccountStatus.ACTIVE)));
        RiskAssessmentResponse response = controlService.assessRisk(assessment("49999.99"));
        assertEquals(response.getDecision(), RiskDecision.ALLOW.name()); assertEquals(response.getControlCaseReference(), null); verify(controlCaseRepository, never()).save(any(ControlCase.class));
    }
    @Test public void opensCaseAndNotificationForManualReviewThreshold() {
        when(accountRepository.findByAccountNumber("ACC000101")).thenReturn(Optional.of(account(AccountStatus.ACTIVE)));
        RiskAssessmentResponse response = controlService.assessRisk(assessment("50000.00"));
        assertEquals(response.getDecision(), RiskDecision.REVIEW.name()); verify(controlCaseRepository).save(any(ControlCase.class)); verify(controlNotificationRepository).save(any(ControlNotification.class));
    }
    @Test public void blocksRiskAssessmentForInactiveAccount() {
        when(accountRepository.findByAccountNumber("ACC000101")).thenReturn(Optional.of(account(AccountStatus.FROZEN)));
        assertEquals(controlService.assessRisk(assessment("10.00")).getDecision(), RiskDecision.BLOCK.name());
    }
    @Test public void resolvesOpenControlCase() {
        ControlCase controlCase = controlCase(ControlCaseStatus.OPEN); when(controlCaseRepository.findByCaseReference("CASE-1")).thenReturn(Optional.of(controlCase));
        ControlCaseResponse response = controlService.resolveCase("CASE-1", resolution());
        assertEquals(response.getStatus(), ControlCaseStatus.RESOLVED.name()); assertEquals(response.getResolvedBy(), "OPS-7001");
    }
    @Test public void marksPendingNotificationAsSent() {
        ControlNotification notification = notification(NotificationStatus.PENDING); when(controlNotificationRepository.findByNotificationReference("NTF-1")).thenReturn(Optional.of(notification));
        ControlNotificationResponse response = controlService.markNotificationSent("NTF-1");
        assertEquals(response.getStatus(), NotificationStatus.SENT.name());
    }
    @Test public void preventsSendingNotificationTwice() {
        ControlNotification notification = notification(NotificationStatus.SENT); when(controlNotificationRepository.findByNotificationReference("NTF-1")).thenReturn(Optional.of(notification));
        expectThrows(ControlWorkflowException.class, () -> controlService.markNotificationSent("NTF-1"));
    }
    private Account account(AccountStatus status) { Account account = new Account(); account.setId(101L); account.setAccountNumber("ACC000101"); account.setAccountStatus(status); return account; }
    private RiskAssessmentRequest assessment(String amount) { RiskAssessmentRequest request = new RiskAssessmentRequest(); request.setAccountNumber("ACC000101"); request.setOperationType("PAYMENT"); request.setOperationReference("PAY-1"); request.setAmount(new BigDecimal(amount)); return request; }
    private ControlCase controlCase(ControlCaseStatus status) { RiskAssessment assessment = new RiskAssessment(); assessment.setAssessmentReference("RISK-1"); assessment.setDecision(RiskDecision.REVIEW); ControlCase controlCase = new ControlCase(); controlCase.setCaseReference("CASE-1"); controlCase.setAssessment(assessment); controlCase.setStatus(status); return controlCase; }
    private ControlCaseResolutionRequest resolution() { ControlCaseResolutionRequest request = new ControlCaseResolutionRequest(); request.setResolvedBy("OPS-7001"); request.setResolutionNotes("Manual review completed."); return request; }
    private ControlNotification notification(NotificationStatus status) { ControlNotification notification = new ControlNotification(); notification.setNotificationReference("NTF-1"); notification.setRecipient("COMPLIANCE_OPERATIONS"); notification.setChannel(NotificationChannel.IN_APP); notification.setStatus(status); notification.setRelatedReference("CASE-1"); notification.setMessage("Review required."); notification.setCreatedAt(LocalDateTime.now()); return notification; }
}
