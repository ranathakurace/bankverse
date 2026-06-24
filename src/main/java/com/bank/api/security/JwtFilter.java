package com.bank.api.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

public class JwtFilter extends OncePerRequestFilter {

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
                Claims claims = JwtUtil.validateToken(token);

                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                // Create authority from role
                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority(role);

                // Create authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                Collections.singleton(authority)
                        );

                // Set authentication in security context
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

            } catch (Exception e) {
                // Invalid or expired token
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}
