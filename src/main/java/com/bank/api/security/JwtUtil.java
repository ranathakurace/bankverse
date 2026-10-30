package com.bank.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final byte[] signingKey;

    // ⏰ Token validity: 30 minutes
    private static final long EXPIRATION_TIME = 1000 * 60 * 30;

    public JwtUtil(@Value("${bankverse.jwt.secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("BANKVERSE_JWT_SECRET must contain at least 32 UTF-8 bytes.");
        }
        this.signingKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generate JWT token with role
     */
    public String generateToken(String username, String role) {

        return generateToken(username, Set.of(role));
    }

    /** Generates an access token containing every effective Spring Security authority. */
    public String generateToken(String username, Set<String> roles) {

        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .claim("role", roles.iterator().next()) // compatibility with tokens issued before multi-role support
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(Keys.hmacShaKeyFor(signingKey))
                .compact();
    }

    /**
     * Validate JWT token and return claims
     */
    public Claims validateToken(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(signingKey))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    // Refresh token validity: 24 hours
    private static final long REFRESH_EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    /**
     * Generate Refresh Token
     */
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setId(UUID.randomUUID().toString())
                .claim("tokenType", "REFRESH")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION_TIME))
                // Use the same raw-byte key representation as generateToken/validateToken.
                .signWith(Keys.hmacShaKeyFor(signingKey), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Validates both the signature and that the JWT is a refresh token, not an access token. */
    public Claims validateRefreshToken(String token) {
        Claims claims = validateToken(token);
        if (!"REFRESH".equals(claims.get("tokenType", String.class))) {
            throw new io.jsonwebtoken.JwtException("JWT is not a refresh token");
        }
        return claims;
    }

}
