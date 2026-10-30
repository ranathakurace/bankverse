package com.bank.api.service;

import com.bank.api.dto.EventConsumptionRequest;
import com.bank.api.dto.EventConsumptionResponse;
import com.bank.api.dto.EventFailureRequest;
import com.bank.api.dto.EventPublishRequest;
import com.bank.api.dto.EventResponse;
import com.bank.api.exception.EventWorkflowException;
import com.bank.api.exception.InvalidEventException;
import com.bank.api.model.EventConsumption;
import com.bank.api.model.EventConsumptionStatus;
import com.bank.api.model.EventOutbox;
import com.bank.api.model.OutboxEventStatus;
import com.bank.api.repository.EventConsumptionRepository;
import com.bank.api.repository.EventOutboxRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Durable event contract for an at-least-once broker adapter.  Dispatch is deliberately
 * broker-neutral: a Kafka/RabbitMQ adapter can replace this step without changing producers
 * or consumers.
 */
@Service
public class EventOutboxService {
    private final EventOutboxRepository outboxRepository;
    private final EventConsumptionRepository consumptionRepository;

    public EventOutboxService(EventOutboxRepository outboxRepository, EventConsumptionRepository consumptionRepository) {
        this.outboxRepository = outboxRepository;
        this.consumptionRepository = consumptionRepository;
    }

    @Transactional
    public EventResponse publish(String idempotencyKey, EventPublishRequest request) {
        validatePublish(idempotencyKey, request);
        String normalizedKey = idempotencyKey.trim();
        EventOutbox existing = outboxRepository.findByIdempotencyKey(normalizedKey).orElse(null);
        if (existing != null) { return response(existing); }
        EventOutbox event = new EventOutbox();
        event.setEventId("EVT-" + UUID.randomUUID());
        event.setAggregateType(request.getAggregateType().trim().toUpperCase());
        event.setAggregateReference(request.getAggregateReference().trim());
        event.setEventType(request.getEventType().trim().toUpperCase());
        event.setPayload(request.getPayload().trim());
        event.setIdempotencyKey(normalizedKey);
        event.setStatus(OutboxEventStatus.PENDING);
        event.setAttemptCount(0);
        event.setCreatedAt(LocalDateTime.now());
        return response(outboxRepository.save(event));
    }

    @Transactional
    public EventResponse dispatch(String eventId) {
        EventOutbox event = find(eventId);
        if (event.getStatus() != OutboxEventStatus.PENDING) { throw new EventWorkflowException("Only PENDING events can be dispatched."); }
        event.setStatus(OutboxEventStatus.PUBLISHED);
        event.setAttemptCount(event.getAttemptCount() + 1);
        event.setPublishedAt(LocalDateTime.now());
        event.setFailureReason(null);
        return response(outboxRepository.save(event));
    }

    @Transactional
    public EventResponse markFailed(String eventId, EventFailureRequest request) {
        if (request == null || isBlank(request.getFailureReason()) || request.getFailureReason().trim().length() > 500) { throw new InvalidEventException("A failure reason of up to 500 characters is mandatory."); }
        EventOutbox event = find(eventId);
        if (event.getStatus() != OutboxEventStatus.PENDING) { throw new EventWorkflowException("Only PENDING events can be marked as failed."); }
        event.setStatus(OutboxEventStatus.FAILED);
        event.setAttemptCount(event.getAttemptCount() + 1);
        event.setFailureReason(request.getFailureReason().trim());
        return response(outboxRepository.save(event));
    }

    @Transactional
    public EventResponse retry(String eventId) {
        EventOutbox event = find(eventId);
        if (event.getStatus() != OutboxEventStatus.FAILED) { throw new EventWorkflowException("Only FAILED events can be retried."); }
        event.setStatus(OutboxEventStatus.PENDING);
        event.setFailureReason(null);
        return response(outboxRepository.save(event));
    }

    @Transactional
    public EventConsumptionResponse consume(String eventId, EventConsumptionRequest request) {
        if (request == null || isBlank(request.getConsumerName()) || request.getConsumerName().trim().length() > 100) { throw new InvalidEventException("A consumer name of up to 100 characters is mandatory."); }
        EventOutbox event = find(eventId);
        if (event.getStatus() != OutboxEventStatus.PUBLISHED) { throw new EventWorkflowException("Only PUBLISHED events can be consumed."); }
        String consumer = request.getConsumerName().trim().toUpperCase();
        EventConsumption existing = consumptionRepository.findByEventIdAndConsumerName(event.getEventId(), consumer).orElse(null);
        if (existing != null) { return consumptionResponse(existing); }
        EventConsumption receipt = new EventConsumption();
        receipt.setEventId(event.getEventId());
        receipt.setConsumerName(consumer);
        receipt.setStatus(EventConsumptionStatus.PROCESSED);
        receipt.setProcessedAt(LocalDateTime.now());
        return consumptionResponse(consumptionRepository.save(receipt));
    }

    @Transactional(readOnly = true)
    public EventResponse get(String eventId) { return response(find(eventId)); }

    private EventOutbox find(String eventId) {
        if (isBlank(eventId)) { throw new InvalidEventException("Event ID is mandatory."); }
        return outboxRepository.findByEventId(eventId.trim()).orElseThrow(() -> new EventWorkflowException("Event was not found."));
    }
    private void validatePublish(String key, EventPublishRequest r) {
        if (isBlank(key) || key.trim().length() > 100) { throw new InvalidEventException("Idempotency-Key of up to 100 characters is mandatory."); }
        if (r == null || isBlank(r.getAggregateType()) || isBlank(r.getAggregateReference()) || isBlank(r.getEventType()) || isBlank(r.getPayload())) { throw new InvalidEventException("Aggregate type, aggregate reference, event type and payload are mandatory."); }
        if (r.getAggregateType().trim().length() > 50 || r.getAggregateReference().trim().length() > 100 || r.getEventType().trim().length() > 100 || r.getPayload().trim().length() > 10000) { throw new InvalidEventException("Event data exceeds its allowed length."); }
    }
    private EventResponse response(EventOutbox e) { EventResponse r = new EventResponse(); r.setEventId(e.getEventId()); r.setAggregateType(e.getAggregateType()); r.setAggregateReference(e.getAggregateReference()); r.setEventType(e.getEventType()); r.setStatus(e.getStatus().name()); r.setAttemptCount(e.getAttemptCount()); r.setFailureReason(e.getFailureReason()); r.setCreatedAt(e.getCreatedAt()); r.setPublishedAt(e.getPublishedAt()); return r; }
    private EventConsumptionResponse consumptionResponse(EventConsumption c) { EventConsumptionResponse r = new EventConsumptionResponse(); r.setEventId(c.getEventId()); r.setConsumerName(c.getConsumerName()); r.setStatus(c.getStatus().name()); r.setProcessedAt(c.getProcessedAt()); return r; }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
