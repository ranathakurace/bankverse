package com.bank.api.model;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "event_consumptions", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "consumer_name"}))
public class EventConsumption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "event_id", nullable = false, length = 50) private String eventId;
    @Column(name = "consumer_name", nullable = false, length = 100) private String consumerName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EventConsumptionStatus status;
    @Column(name = "processed_at", nullable = false) private LocalDateTime processedAt;
}
