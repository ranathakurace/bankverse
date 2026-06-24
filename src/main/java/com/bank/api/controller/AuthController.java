package com.bank.api.controller;

import com.bank.api.security.JwtUtil;
import com.bank.api.security.RefreshTokenStore;
import io.jsonwebtoken.Claims;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthController {

    /**
     * LOGIN API
     * Returns Access Token + Refresh Token
     */
    @PostMapping("/auth/login")
    public Map<String, String> login(@RequestBody Map<String, String> request) {

        String username = request.get("username");
        String password = request.get("password");

        String role;

        // Hardcoded users (for learning & testing)
        if ("user".equals(username) && "password".equals(password)) {
            role = "ROLE_USER";
        } else if ("admin".equals(username) && "password".equals(password)) {
            role = "ROLE_ADMIN";
        } else {
            throw new RuntimeException("Invalid credentials");
        }

        // Generate tokens
        String accessToken = JwtUtil.generateToken(username, role);
        String refreshToken = JwtUtil.generateRefreshToken(username);

        // Store refresh token
        RefreshTokenStore.save(username, refreshToken);

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);

        return response;
    }

    /**
     * REFRESH TOKEN API
     * Returns new Access Token
     */
    @PostMapping("/auth/refresh")
    public Map<String, String> refresh(@RequestBody Map<String, String> request) {

        String refreshToken = request.get("refreshToken");

        try {
            Claims claims = JwtUtil.validateToken(refreshToken);
            String username = claims.getSubject();

            String storedToken = RefreshTokenStore.get(username);

            if (storedToken == null || !storedToken.equals(refreshToken)) {
                throw new RuntimeException("Invalid refresh token");
            }

            // NOTE: Role would come from DB in real apps
            String role = username.equals("admin") ? "ROLE_ADMIN" : "ROLE_USER";

            String newAccessToken = JwtUtil.generateToken(username, role);

            Map<String, String> response = new HashMap<>();
            response.put("accessToken", newAccessToken);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Refresh token expired or invalid");
        }
    }
}
