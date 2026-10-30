package com.bank.api.controller;

import com.bank.api.dto.EventConsumptionRequest;
import com.bank.api.dto.EventConsumptionResponse;
import com.bank.api.dto.EventFailureRequest;
import com.bank.api.dto.EventPublishRequest;
import com.bank.api.dto.EventResponse;
import com.bank.api.service.EventOutboxService;
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
@RequestMapping("/api/v1/events")
public class EventOutboxController {
    private final EventOutboxService eventOutboxService;
    public EventOutboxController(EventOutboxService eventOutboxService) { this.eventOutboxService = eventOutboxService; }
    @PostMapping public ResponseEntity<EventResponse> publish(@RequestHeader(value = "Idempotency-Key", required = false) String key, @RequestBody EventPublishRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(eventOutboxService.publish(key, request)); }
    @GetMapping("/{eventId}") public ResponseEntity<EventResponse> get(@PathVariable String eventId) { return ResponseEntity.ok(eventOutboxService.get(eventId)); }
    @PostMapping("/{eventId}/dispatch") public ResponseEntity<EventResponse> dispatch(@PathVariable String eventId) { return ResponseEntity.ok(eventOutboxService.dispatch(eventId)); }
    @PostMapping("/{eventId}/fail") public ResponseEntity<EventResponse> fail(@PathVariable String eventId, @RequestBody EventFailureRequest request) { return ResponseEntity.ok(eventOutboxService.markFailed(eventId, request)); }
    @PostMapping("/{eventId}/retry") public ResponseEntity<EventResponse> retry(@PathVariable String eventId) { return ResponseEntity.ok(eventOutboxService.retry(eventId)); }
    @PostMapping("/{eventId}/consume") public ResponseEntity<EventConsumptionResponse> consume(@PathVariable String eventId, @RequestBody EventConsumptionRequest request) { return ResponseEntity.ok(eventOutboxService.consume(eventId, request)); }
}
