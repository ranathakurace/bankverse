package com.bank.api.dto;

import lombok.Data;

@Data
public class CardStatusChangeRequest {
    private String performedBy;
    private String reason;
}
