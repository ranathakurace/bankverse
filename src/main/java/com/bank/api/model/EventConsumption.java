package com.bank.api.model;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
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
