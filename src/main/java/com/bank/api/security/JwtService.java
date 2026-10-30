package com.bank.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
public class JwtService {
    private final byte[] signingKey;
    public JwtService(@Value("${bankverse.jwt.secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("BANKVERSE_JWT_SECRET must contain at least 32 UTF-8 bytes.");
        }
        this.signingKey = secret.getBytes(StandardCharsets.UTF_8);
    }
    public String createAccessToken(String username) { return create(username, 30 * 60 * 1000L); }
    public String createRefreshToken(String username) { return create(username, 24 * 60 * 60 * 1000L); }
    public Claims validate(String token) { return Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(signingKey)).build().parseClaimsJws(token).getBody(); }
    private String create(String username, long lifetime) { Date now = new Date(); return Jwts.builder().setSubject(username).setIssuedAt(now).setExpiration(new Date(now.getTime() + lifetime)).signWith(Keys.hmacShaKeyFor(signingKey)).compact(); }
}
