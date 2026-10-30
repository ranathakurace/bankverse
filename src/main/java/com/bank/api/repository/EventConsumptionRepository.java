package com.bank.api.repository;

import com.bank.api.model.EventConsumption;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventConsumptionRepository extends JpaRepository<EventConsumption, Long> {
    Optional<EventConsumption> findByEventIdAndConsumerName(String eventId, String consumerName);
}
