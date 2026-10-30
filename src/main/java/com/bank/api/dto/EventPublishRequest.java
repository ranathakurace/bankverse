package com.bank.api.dto;

import lombok.Data;

@Data
public class EventPublishRequest {
    private String aggregateType;
    private String aggregateReference;
    private String eventType;
    private String payload;
}
