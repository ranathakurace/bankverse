package com.bank.api.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EventConsumptionResponse {
    private String eventId; private String consumerName; private String status; private LocalDateTime processedAt;
}
