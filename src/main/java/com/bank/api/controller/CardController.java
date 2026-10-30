package com.bank.api.controller;

import com.bank.api.dto.CardIssueRequest;
import com.bank.api.dto.CardResponse;
import com.bank.api.dto.CardStatusChangeRequest;
import com.bank.api.service.CardService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cards")
public class CardController {
    private final CardService cardService;
    public CardController(CardService cardService) { this.cardService = cardService; }
    @PostMapping public ResponseEntity<CardResponse> issue(@RequestBody CardIssueRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(cardService.issueCard(request)); }
    @PostMapping("/{cardReference}/activate") public ResponseEntity<CardResponse> activate(@PathVariable String cardReference, @RequestBody CardStatusChangeRequest request) { return ResponseEntity.ok(cardService.activateCard(cardReference, request)); }
    @PostMapping("/{cardReference}/block") public ResponseEntity<CardResponse> block(@PathVariable String cardReference, @RequestBody CardStatusChangeRequest request) { return ResponseEntity.ok(cardService.blockCard(cardReference, request)); }
    @PostMapping("/{cardReference}/unblock") public ResponseEntity<CardResponse> unblock(@PathVariable String cardReference, @RequestBody CardStatusChangeRequest request) { return ResponseEntity.ok(cardService.unblockCard(cardReference, request)); }
    @PostMapping("/{cardReference}/close") public ResponseEntity<CardResponse> close(@PathVariable String cardReference, @RequestBody CardStatusChangeRequest request) { return ResponseEntity.ok(cardService.closeCard(cardReference, request)); }
    @GetMapping("/{cardReference}") public ResponseEntity<CardResponse> get(@PathVariable String cardReference) { return ResponseEntity.ok(cardService.getCard(cardReference)); }
    @GetMapping("/customer/{customerNumber}") public ResponseEntity<List<CardResponse>> customerCards(@PathVariable String customerNumber) { return ResponseEntity.ok(cardService.getCustomerCards(customerNumber)); }
}
