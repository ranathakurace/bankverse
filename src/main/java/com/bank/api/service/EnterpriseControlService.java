package com.bank.api.service;

import com.bank.api.dto.ControlCaseResolutionRequest;
import com.bank.api.dto.ControlCaseResponse;
import com.bank.api.dto.ControlNotificationResponse;
import com.bank.api.dto.RiskAssessmentRequest;
import com.bank.api.dto.RiskAssessmentResponse;
import com.bank.api.exception.AccountNotFoundException;
import com.bank.api.exception.ControlWorkflowException;
import com.bank.api.exception.InvalidControlException;
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
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnterpriseControlService {
    private static final String ACCOUNT_NUMBER_REGEX = "^ACC\\d{6}$";
    private final AccountRepository accountRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final ControlCaseRepository controlCaseRepository;
    private final ControlNotificationRepository controlNotificationRepository;
    public EnterpriseControlService(AccountRepository accountRepository, RiskAssessmentRepository riskAssessmentRepository,
            ControlCaseRepository controlCaseRepository, ControlNotificationRepository controlNotificationRepository) {
        this.accountRepository = accountRepository; this.riskAssessmentRepository = riskAssessmentRepository;
        this.controlCaseRepository = controlCaseRepository; this.controlNotificationRepository = controlNotificationRepository;
    }
    @Transactional
    public RiskAssessmentResponse assessRisk(RiskAssessmentRequest request) {
        validateAssessment(request); Account account = findAccount(request.getAccountNumber());
        RiskDecision decision; int score; String reason;
        if (account.getAccountStatus() != AccountStatus.ACTIVE) { decision = RiskDecision.BLOCK; score = 95; reason = "Source account is not active."; }
        else if (request.getAmount().compareTo(new BigDecimal("100000.00")) >= 0) { decision = RiskDecision.BLOCK; score = 90; reason = "Amount meets the high-risk blocking threshold."; }
        else if (request.getAmount().compareTo(new BigDecimal("50000.00")) >= 0) { decision = RiskDecision.REVIEW; score = 60; reason = "Amount meets the manual-review threshold."; }
        else { decision = RiskDecision.ALLOW; score = 15; reason = "Amount is within the configured low-risk threshold."; }
        RiskAssessment assessment = new RiskAssessment(); assessment.setAssessmentReference("RISK-" + UUID.randomUUID()); assessment.setAccount(account); assessment.setOperationType(request.getOperationType().trim().toUpperCase(Locale.ROOT)); assessment.setOperationReference(trimOptional(request.getOperationReference())); assessment.setAmount(request.getAmount()); assessment.setRiskScore(score); assessment.setDecision(decision); assessment.setReason(reason); assessment.setAssessedAt(LocalDateTime.now()); RiskAssessment saved = riskAssessmentRepository.save(assessment);
        ControlCase controlCase = null;
        if (decision != RiskDecision.ALLOW) { controlCase = openCase(saved); createNotification(controlCase.getCaseReference(), "Risk assessment requires " + decision + ": " + reason); }
        return assessmentResponse(saved, controlCase);
    }
    @Transactional
    public ControlCaseResponse resolveCase(String caseReference, ControlCaseResolutionRequest request) {
        if (isBlank(caseReference)) { throw new InvalidControlException("Control case reference is mandatory."); }
        validateResolution(request); ControlCase controlCase = controlCaseRepository.findByCaseReference(caseReference.trim()).orElseThrow(() -> new ControlWorkflowException("Control case was not found."));
        if (controlCase.getStatus() != ControlCaseStatus.OPEN) { throw new ControlWorkflowException("Only OPEN control cases can be resolved."); }
        controlCase.setStatus(ControlCaseStatus.RESOLVED); controlCase.setResolvedAt(LocalDateTime.now()); controlCase.setResolvedBy(request.getResolvedBy().trim()); controlCase.setResolutionNotes(request.getResolutionNotes().trim()); controlCaseRepository.save(controlCase); createNotification(controlCase.getCaseReference(), "Control case resolved by " + controlCase.getResolvedBy() + "."); return caseResponse(controlCase);
    }
    @Transactional
    public ControlNotificationResponse markNotificationSent(String notificationReference) {
        if (isBlank(notificationReference)) { throw new InvalidControlException("Notification reference is mandatory."); }
        ControlNotification notification = controlNotificationRepository.findByNotificationReference(notificationReference.trim()).orElseThrow(() -> new ControlWorkflowException("Control notification was not found."));
        if (notification.getStatus() != NotificationStatus.PENDING) { throw new ControlWorkflowException("Only PENDING notifications can be marked as sent."); }
        notification.setStatus(NotificationStatus.SENT); notification.setSentAt(LocalDateTime.now()); controlNotificationRepository.save(notification); return notificationResponse(notification);
    }
    @Transactional(readOnly = true) public ControlCaseResponse getCase(String ref) { return caseResponse(controlCaseRepository.findByCaseReference(ref).orElseThrow(() -> new ControlWorkflowException("Control case was not found."))); }
    private ControlCase openCase(RiskAssessment assessment) { ControlCase c = new ControlCase(); c.setCaseReference("CASE-" + UUID.randomUUID()); c.setAssessment(assessment); c.setStatus(ControlCaseStatus.OPEN); c.setOpenedAt(LocalDateTime.now()); return controlCaseRepository.save(c); }
    private void createNotification(String relatedReference, String message) { ControlNotification n = new ControlNotification(); n.setNotificationReference("NTF-" + UUID.randomUUID()); n.setRecipient("COMPLIANCE_OPERATIONS"); n.setChannel(NotificationChannel.IN_APP); n.setStatus(NotificationStatus.PENDING); n.setMessage(message); n.setRelatedReference(relatedReference); n.setCreatedAt(LocalDateTime.now()); controlNotificationRepository.save(n); }
    private Account findAccount(String no) { if (isBlank(no)) { throw new InvalidControlException("Account number is mandatory."); } String normalized = no.trim().toUpperCase(Locale.ROOT); if (!normalized.matches(ACCOUNT_NUMBER_REGEX)) { throw new InvalidControlException("Invalid account number format."); } return accountRepository.findByAccountNumber(normalized).orElseThrow(() -> new AccountNotFoundException(normalized)); }
    private void validateAssessment(RiskAssessmentRequest r) { if (r == null || isBlank(r.getOperationType())) { throw new InvalidControlException("Account number and operation type are mandatory."); } findAccountFormat(r.getAccountNumber()); if (r.getOperationType().trim().length() > 50) { throw new InvalidControlException("Operation type cannot exceed 50 characters."); } if (r.getOperationReference() != null && r.getOperationReference().trim().length() > 100) { throw new InvalidControlException("Operation reference cannot exceed 100 characters."); } if (r.getAmount() == null || r.getAmount().signum() <= 0 || r.getAmount().scale() > 2) { throw new InvalidControlException("Amount must be greater than zero with at most two decimal places."); } }
    private void findAccountFormat(String no) { if (isBlank(no) || !no.trim().toUpperCase(Locale.ROOT).matches(ACCOUNT_NUMBER_REGEX)) { throw new InvalidControlException("Invalid account number format."); } }
    private void validateResolution(ControlCaseResolutionRequest r) { if (r == null || isBlank(r.getResolvedBy()) || isBlank(r.getResolutionNotes())) { throw new InvalidControlException("Resolved by and resolution notes are mandatory."); } if (r.getResolvedBy().trim().length() > 100 || r.getResolutionNotes().trim().length() > 500) { throw new InvalidControlException("Resolution data exceeds the allowed length."); } }
    private RiskAssessmentResponse assessmentResponse(RiskAssessment a, ControlCase c) { RiskAssessmentResponse r = new RiskAssessmentResponse(); r.setAssessmentReference(a.getAssessmentReference()); r.setAccountNumber(a.getAccount().getAccountNumber()); r.setRiskScore(a.getRiskScore()); r.setDecision(a.getDecision().name()); r.setReason(a.getReason()); r.setControlCaseReference(c == null ? null : c.getCaseReference()); return r; }
    private ControlCaseResponse caseResponse(ControlCase c) { ControlCaseResponse r = new ControlCaseResponse(); r.setCaseReference(c.getCaseReference()); r.setAssessmentReference(c.getAssessment().getAssessmentReference()); r.setStatus(c.getStatus().name()); r.setDecision(c.getAssessment().getDecision().name()); r.setResolvedBy(c.getResolvedBy()); r.setResolutionNotes(c.getResolutionNotes()); return r; }
    private ControlNotificationResponse notificationResponse(ControlNotification n) { ControlNotificationResponse r = new ControlNotificationResponse(); r.setNotificationReference(n.getNotificationReference()); r.setRecipient(n.getRecipient()); r.setChannel(n.getChannel().name()); r.setStatus(n.getStatus().name()); r.setRelatedReference(n.getRelatedReference()); return r; }
    private String trimOptional(String v) { return v == null ? null : v.trim(); } private boolean isBlank(String v) { return v == null || v.trim().isEmpty(); }
}
