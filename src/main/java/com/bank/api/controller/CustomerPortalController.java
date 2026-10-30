package com.bank.api.controller;

import com.bank.api.dto.AccountResponse;
import com.bank.api.dto.FundTransferRequest;
import com.bank.api.dto.FundTransferResponse;
import com.bank.api.dto.TransactionHistoryResponse;
import com.bank.api.service.CustomerPortalService;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer-portal")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerPortalController {
    private final CustomerPortalService portal;
    public CustomerPortalController(CustomerPortalService portal) { this.portal = portal; }
    @GetMapping("/accounts") public List<AccountResponse> accounts(Principal principal) { return portal.accounts(principal.getName()); }
    @GetMapping("/accounts/{accountNumber}/transactions") public TransactionHistoryResponse history(@PathVariable String accountNumber, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, Principal principal) { return portal.history(principal.getName(), accountNumber, page, size); }
    @PostMapping("/transfers") public ResponseEntity<FundTransferResponse> transfer(@RequestHeader("Idempotency-Key") String key, @RequestBody FundTransferRequest request, Principal principal) { return ResponseEntity.status(HttpStatus.CREATED).body(portal.transfer(principal.getName(), key, request)); }
}
