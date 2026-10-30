package com.bank.api.controller;

import com.bank.api.dto.PaymentActionRequest;
import com.bank.api.dto.PaymentInitiationRequest;
import com.bank.api.dto.PaymentResponse;
import com.bank.api.service.PaymentService;
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
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;
    public PaymentController(PaymentService paymentService) { this.paymentService = paymentService; }
    @PostMapping public ResponseEntity<PaymentResponse> initiate(@RequestHeader("Idempotency-Key") String key, @RequestBody PaymentInitiationRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.initiate(key, request)); }
    @PostMapping("/{paymentReference}/authorize") public ResponseEntity<PaymentResponse> authorize(@PathVariable String paymentReference, @RequestBody PaymentActionRequest request) { return ResponseEntity.ok(paymentService.authorize(paymentReference, request)); }
    @PostMapping("/{paymentReference}/execute") public ResponseEntity<PaymentResponse> execute(@PathVariable String paymentReference, @RequestBody PaymentActionRequest request) { return ResponseEntity.ok(paymentService.execute(paymentReference, request)); }
    @PostMapping("/{paymentReference}/fail") public ResponseEntity<PaymentResponse> fail(@PathVariable String paymentReference, @RequestBody PaymentActionRequest request) { return ResponseEntity.ok(paymentService.fail(paymentReference, request)); }
    @PostMapping("/{paymentReference}/retry") public ResponseEntity<PaymentResponse> retry(@PathVariable String paymentReference, @RequestBody PaymentActionRequest request) { return ResponseEntity.ok(paymentService.retry(paymentReference, request)); }
    @PostMapping("/{paymentReference}/refund") public ResponseEntity<PaymentResponse> refund(@PathVariable String paymentReference, @RequestBody PaymentActionRequest request) { return ResponseEntity.ok(paymentService.refund(paymentReference, request)); }
    @GetMapping("/{paymentReference}") public ResponseEntity<PaymentResponse> get(@PathVariable String paymentReference) { return ResponseEntity.ok(paymentService.get(paymentReference)); }
}
