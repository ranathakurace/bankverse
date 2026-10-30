package com.bank.api.repository;

import com.bank.api.model.EventOutbox;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventOutboxRepository extends JpaRepository<EventOutbox, Long> {
    Optional<EventOutbox> findByEventId(String eventId);
    Optional<EventOutbox> findByIdempotencyKey(String idempotencyKey);
}
