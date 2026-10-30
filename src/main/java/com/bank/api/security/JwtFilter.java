package com.bank.api.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /** Authentication endpoints must remain usable when a browser has an expired session token. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/bootstrap") || path.equals("/auth/login") || path.equals("/auth/refresh")
                || path.equals("/customer-auth/enroll") || path.equals("/customer-auth/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Check if Authorization header is present
        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            try {
                // Validate token
                Claims claims = jwtUtil.validateToken(token);

                String username = claims.getSubject();
                Object rolesClaim = claims.get("roles");
                List<SimpleGrantedAuthority> authorities;
                if (rolesClaim instanceof Collection<?>) {
                    authorities = ((Collection<?>) rolesClaim).stream()
                            .map(String::valueOf).map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                } else {
                    String role = claims.get("role", String.class);
                    authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
                }

                // Create authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                authorities
                        );

                // Set authentication in security context
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

            } catch (Exception e) {
                // Invalid or expired token
                ApiSecurityErrorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                        "INVALID_ACCESS_TOKEN", "The supplied access token is invalid or expired.");
                return;
            }
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}
