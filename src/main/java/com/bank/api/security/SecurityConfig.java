package com.bank.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter.XFrameOptionsMode;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CorrelationIdFilter correlationIdFilter;
    private final JwtUtil jwtUtil;

    public SecurityConfig(RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler, CorrelationIdFilter correlationIdFilter, JwtUtil jwtUtil) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.correlationIdFilter = correlationIdFilter;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public JwtFilter jwtFilter() {
        return new JwtFilter(jwtUtil);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(
                    "/auth/bootstrap/**",
                    "/auth/login",
                    "/auth/refresh",
                    "/customer-auth/enroll",
                    "/customer-auth/login",
                    "/health",
                    "/",
                    "/index.html",
                    "/automation-frame.html",
                    "/automation-popup.html",
                    "/assets/**"
            ).permitAll()
            .requestMatchers("/api/v1/customer-portal/**").hasRole("CUSTOMER")
            .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/customers/**"
            ).hasAnyRole("CUSTOMER_OFFICER", "KYC_OFFICER", "ACCOUNT_OFFICER", "OPERATIONS_MANAGER", "AUDITOR", "ADMIN")
            .requestMatchers(
                    HttpMethod.POST,
                    "/bank/account"
            ).permitAll()
            .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/customers/register"
            ).hasAnyRole("CUSTOMER_OFFICER", "ADMIN")
            .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/customers/*/kyc/**"
            ).hasAnyRole("KYC_OFFICER", "ADMIN")
            .requestMatchers("/api/v1/accounts/**", "/api/v1/cards/**", "/api/v1/loans/**")
            .hasAnyRole("ACCOUNT_OFFICER", "ADMIN")
            .requestMatchers("/api/v1/transactions/**", "/api/v1/payments/**", "/api/v1/events/**")
            .hasAnyRole("OPERATIONS_MANAGER", "ADMIN")
            .requestMatchers("/api/v1/controls/**")
            .hasAnyRole("OPERATIONS_MANAGER", "AUDITOR", "ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class);

        // Only the embedded training document may be framed by this application's own origin.
        AntPathRequestMatcher automationFrame = new AntPathRequestMatcher("/automation-frame.html");
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable())
            .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(automationFrame,
                new XFrameOptionsHeaderWriter(XFrameOptionsMode.SAMEORIGIN)))
            .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(new NegatedRequestMatcher(automationFrame),
                new XFrameOptionsHeaderWriter(XFrameOptionsMode.DENY))));

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
