package com.bank.api.controller;

import com.bank.api.dto.ControlCaseResolutionRequest;
import com.bank.api.dto.ControlCaseResponse;
import com.bank.api.dto.ControlNotificationResponse;
import com.bank.api.dto.RiskAssessmentRequest;
import com.bank.api.dto.RiskAssessmentResponse;
import com.bank.api.service.EnterpriseControlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/controls")
public class EnterpriseControlController {
    private final EnterpriseControlService enterpriseControlService;
    public EnterpriseControlController(EnterpriseControlService enterpriseControlService) { this.enterpriseControlService = enterpriseControlService; }
    @PostMapping("/risk-assessments") public ResponseEntity<RiskAssessmentResponse> assess(@RequestBody RiskAssessmentRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(enterpriseControlService.assessRisk(request)); }
    @GetMapping("/cases/{caseReference}") public ResponseEntity<ControlCaseResponse> getCase(@PathVariable String caseReference) { return ResponseEntity.ok(enterpriseControlService.getCase(caseReference)); }
    @PostMapping("/cases/{caseReference}/resolve") public ResponseEntity<ControlCaseResponse> resolve(@PathVariable String caseReference, @RequestBody ControlCaseResolutionRequest request) { return ResponseEntity.ok(enterpriseControlService.resolveCase(caseReference, request)); }
    @PostMapping("/notifications/{notificationReference}/sent") public ResponseEntity<ControlNotificationResponse> markSent(@PathVariable String notificationReference) { return ResponseEntity.ok(enterpriseControlService.markNotificationSent(notificationReference)); }
}
