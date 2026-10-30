package com.bank.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

import com.bank.api.dto.EventConsumptionRequest;
import com.bank.api.dto.EventConsumptionResponse;
import com.bank.api.dto.EventFailureRequest;
import com.bank.api.dto.EventPublishRequest;
import com.bank.api.dto.EventResponse;
import com.bank.api.exception.EventWorkflowException;
import com.bank.api.model.EventConsumption;
import com.bank.api.model.EventOutbox;
import com.bank.api.model.OutboxEventStatus;
import com.bank.api.repository.EventConsumptionRepository;
import com.bank.api.repository.EventOutboxRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EventOutboxServiceTest {
    private EventOutboxRepository outboxRepository; private EventConsumptionRepository consumptionRepository; private EventOutboxService service;
    @BeforeMethod public void setUp() { outboxRepository = mock(EventOutboxRepository.class); consumptionRepository = mock(EventConsumptionRepository.class); service = new EventOutboxService(outboxRepository, consumptionRepository); when(outboxRepository.save(any(EventOutbox.class))).thenAnswer(i -> i.getArgument(0)); when(consumptionRepository.save(any(EventConsumption.class))).thenAnswer(i -> i.getArgument(0)); }
    @Test public void publishesDurablePendingEvent() { when(outboxRepository.findByIdempotencyKey("event-key-1")).thenReturn(Optional.empty()); EventResponse response = service.publish("event-key-1", request()); assertEquals(response.getStatus(), OutboxEventStatus.PENDING.name()); assertEquals(response.getAttemptCount(), Integer.valueOf(0)); verify(outboxRepository).save(any(EventOutbox.class)); }
    @Test public void returnsExistingEventForSameIdempotencyKey() { EventOutbox existing = event(OutboxEventStatus.PENDING); when(outboxRepository.findByIdempotencyKey("event-key-1")).thenReturn(Optional.of(existing)); assertEquals(service.publish("event-key-1", request()).getEventId(), "EVT-1"); verify(outboxRepository, org.mockito.Mockito.never()).save(any(EventOutbox.class)); }
    @Test public void dispatchesPendingEventAndIncrementsAttempt() { EventOutbox event = event(OutboxEventStatus.PENDING); when(outboxRepository.findByEventId("EVT-1")).thenReturn(Optional.of(event)); EventResponse response = service.dispatch("EVT-1"); assertEquals(response.getStatus(), OutboxEventStatus.PUBLISHED.name()); assertEquals(response.getAttemptCount(), Integer.valueOf(1)); }
    @Test public void preventsDispatchOfPublishedEvent() { when(outboxRepository.findByEventId("EVT-1")).thenReturn(Optional.of(event(OutboxEventStatus.PUBLISHED))); expectThrows(EventWorkflowException.class, () -> service.dispatch("EVT-1")); }
    @Test public void supportsFailureAndRetryLifecycle() { EventOutbox event = event(OutboxEventStatus.PENDING); when(outboxRepository.findByEventId("EVT-1")).thenReturn(Optional.of(event)); EventFailureRequest failure = new EventFailureRequest(); failure.setFailureReason("Broker unavailable."); assertEquals(service.markFailed("EVT-1", failure).getStatus(), OutboxEventStatus.FAILED.name()); assertEquals(service.retry("EVT-1").getStatus(), OutboxEventStatus.PENDING.name()); }
    @Test public void recordsConsumerOnceForAtLeastOnceDelivery() { EventOutbox event = event(OutboxEventStatus.PUBLISHED); when(outboxRepository.findByEventId("EVT-1")).thenReturn(Optional.of(event)); when(consumptionRepository.findByEventIdAndConsumerName(eq("EVT-1"), eq("NOTIFICATION-SERVICE"))).thenReturn(Optional.empty()); EventConsumptionRequest request = new EventConsumptionRequest(); request.setConsumerName("notification-service"); EventConsumptionResponse response = service.consume("EVT-1", request); assertEquals(response.getStatus(), "PROCESSED"); assertEquals(response.getConsumerName(), "NOTIFICATION-SERVICE"); }
    @Test public void preventsConsumptionBeforePublish() { when(outboxRepository.findByEventId("EVT-1")).thenReturn(Optional.of(event(OutboxEventStatus.PENDING))); EventConsumptionRequest request = new EventConsumptionRequest(); request.setConsumerName("notification-service"); expectThrows(EventWorkflowException.class, () -> service.consume("EVT-1", request)); }
    private EventPublishRequest request() { EventPublishRequest r = new EventPublishRequest(); r.setAggregateType("PAYMENT"); r.setAggregateReference("PAY-1"); r.setEventType("PAYMENT_COMPLETED"); r.setPayload("{\"paymentReference\":\"PAY-1\"}"); return r; }
    private EventOutbox event(OutboxEventStatus status) { EventOutbox event = new EventOutbox(); event.setEventId("EVT-1"); event.setAggregateType("PAYMENT"); event.setAggregateReference("PAY-1"); event.setEventType("PAYMENT_COMPLETED"); event.setPayload("{}"); event.setIdempotencyKey("event-key-1"); event.setStatus(status); event.setAttemptCount(0); event.setCreatedAt(LocalDateTime.now()); return event; }
}
