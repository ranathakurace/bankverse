package com.bank.api.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/** Credentials supplied to the employee authentication endpoint. */
@Data
public class LoginRequest {
    @NotBlank(message = "username is mandatory")
    private String username;

    @NotBlank(message = "password is mandatory")
    private String password;
}
