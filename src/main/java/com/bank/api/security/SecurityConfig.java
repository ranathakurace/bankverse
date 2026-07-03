package com.bank.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public JwtFilter jwtFilter() {
        return new JwtFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // Disable CSRF for APIs
            .csrf().disable()

            // Stateless session (JWT)
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()

            // Authorization rules
            .authorizeRequests()
            .antMatchers(
                    "/auth/**",
                    "/health"
            ).permitAll()
            .antMatchers(
                    HttpMethod.GET,
                    "/api/v1/customers/**"
            ).permitAll()
            .antMatchers(
                    HttpMethod.POST,
                    "/bank/account"
            ).permitAll()
                .anyRequest().authenticated()
            .and()

            // Add JWT filter
            .addFilterBefore(jwtFilter(),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Optional: required by Spring Security internally
    @Bean
    public AuthenticationManager authenticationManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
