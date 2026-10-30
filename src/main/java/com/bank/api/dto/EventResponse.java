package com.bank.api.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EventResponse {
    private String eventId; private String aggregateType; private String aggregateReference;
    private String eventType; private String status; private Integer attemptCount;
    private String failureReason; private LocalDateTime createdAt; private LocalDateTime publishedAt;
}
