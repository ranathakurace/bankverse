package com.bank.api.dto;
import java.util.Set;
import lombok.Data;
@Data public class AuthResponse { private String accessToken; private String refreshToken; private String username; private Set<String> roles; private Set<String> permissions; }
