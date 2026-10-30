package com.bank.api.controller;

import com.bank.api.dto.LoanApplicationRequest;
import com.bank.api.dto.LoanDecisionRequest;
import com.bank.api.dto.LoanDisbursementRequest;
import com.bank.api.dto.LoanResponse;
import com.bank.api.service.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {
    private final LoanService loanService;
    public LoanController(LoanService loanService) { this.loanService = loanService; }
    @PostMapping("/applications") public ResponseEntity<LoanResponse> apply(@RequestBody LoanApplicationRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(loanService.apply(request)); }
    @PostMapping("/{loanReference}/approve") public ResponseEntity<LoanResponse> approve(@PathVariable String loanReference, @RequestBody LoanDecisionRequest request) { return ResponseEntity.ok(loanService.approve(loanReference, request)); }
    @PostMapping("/{loanReference}/reject") public ResponseEntity<LoanResponse> reject(@PathVariable String loanReference, @RequestBody LoanDecisionRequest request) { return ResponseEntity.ok(loanService.reject(loanReference, request)); }
    @PostMapping("/{loanReference}/disburse") public ResponseEntity<LoanResponse> disburse(@PathVariable String loanReference, @RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody LoanDisbursementRequest request) { return ResponseEntity.ok(loanService.disburse(loanReference, idempotencyKey, request)); }
    @GetMapping("/{loanReference}") public ResponseEntity<LoanResponse> get(@PathVariable String loanReference) { return ResponseEntity.ok(loanService.getLoan(loanReference)); }
}
