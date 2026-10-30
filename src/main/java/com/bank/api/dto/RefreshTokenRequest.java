package com.bank.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Payload accepted by the access-token refresh endpoint. */
@Data
public class RefreshTokenRequest {
    @NotBlank(message = "refreshToken is mandatory")
    private String refreshToken;
}
