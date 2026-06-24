package com.bank.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import io.jsonwebtoken.security.Keys;


import java.util.Date;

public class JwtUtil {

    // 🔐 Secret key (for learning purpose only)
    private static final String SECRET_KEY = "ThisIsMyVeryLongSecretKeyForJwtTokenGenerationAndValidation12345678901234567890";

    // ⏰ Token validity: 30 minutes
    private static final long EXPIRATION_TIME = 1000 * 60 * 30;

    /**
     * Generate JWT token with role
     */
    public static String generateToken(String username, String role) {

    	return Jwts.builder()
    	        .setSubject(username)
    	        .claim("role", role)
    	        .setIssuedAt(new Date())
    	        .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
    	        .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
    	        .compact();
    }

    /**
     * Validate JWT token and return claims
     */
    public static Claims validateToken(String token) {

    	return Jwts.parserBuilder()
    	        .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
    	        .build()
    	        .parseClaimsJws(token)
    	        .getBody();
    }
 // Refresh token validity: 24 hours
    private static final long REFRESH_EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    /**
     * Generate Refresh Token
     */
    public static String generateRefreshToken(String username) {

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

}
