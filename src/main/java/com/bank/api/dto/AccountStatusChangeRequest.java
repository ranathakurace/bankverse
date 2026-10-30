package com.bank.api.dto;

import lombok.Data;

@Data
public class AccountStatusChangeRequest {

    private String performedBy;
    private String reason;
}
