package com.bank.api.controller;

import com.bank.api.dto.EmployeeCreateRequest;
import com.bank.api.dto.LoginRequest;
import com.bank.api.dto.PasswordChangeRequest;
import com.bank.api.dto.RefreshTokenRequest;
import com.bank.api.exception.AuthenticationException;
import com.bank.api.exception.EmployeeAdministrationException;
import com.bank.api.model.Employee;
import com.bank.api.security.BankingRole;
import com.bank.api.security.JwtUtil;
import com.bank.api.security.RefreshTokenStore;
import com.bank.api.service.EmployeeService;
import io.jsonwebtoken.Claims;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
public class AuthController {
    private final EmployeeService employees;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(EmployeeService employees, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.employees = employees;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/auth/bootstrap/status")
    public Map<String, Boolean> bootstrapStatus() { return java.util.Collections.singletonMap("bootstrapRequired", employees.bootstrapRequired()); }

    @PostMapping("/auth/bootstrap")
    public Object bootstrap(@RequestBody EmployeeCreateRequest request) { return employees.bootstrap(request); }

    /**
     * LOGIN API
     * Returns Access Token + Refresh Token
     */
    @PostMapping("/auth/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {

        if (employees.bootstrapRequired()) throw new AuthenticationException("Initial administrator setup is required.");
        Employee employee;
        try { employee = employees.findActive(request.getUsername()); }
        catch (EmployeeAdministrationException exception) { throw new AuthenticationException("Invalid credentials."); }
        if (!passwordEncoder.matches(request.getPassword(), employee.getPasswordHash())) throw new AuthenticationException("Invalid credentials.");
        return authenticationResponse(employee);
    }

    /**
     * REFRESH TOKEN API
     * Returns new Access Token
     */
    @PostMapping("/auth/refresh")
    public Map<String, Object> refresh(@Valid @RequestBody RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();

        try {
            Claims claims = jwtUtil.validateRefreshToken(refreshToken);
            String username = claims.getSubject();

            String storedToken = RefreshTokenStore.get(username);

            if (storedToken == null || !storedToken.equals(refreshToken)) {
                throw new RuntimeException("Invalid refresh token");
            }

            return authenticationResponse(employees.findActive(username));

        } catch (Exception e) {
            throw new AuthenticationException("Refresh token expired or invalid.");
        }
    }

    @PostMapping("/auth/password")
    public Map<String, String> changePassword(@RequestBody PasswordChangeRequest request, Principal principal) {
        employees.changePassword(principal.getName(), request.getCurrentPassword(), request.getNewPassword());
        return java.util.Collections.singletonMap("message", "Password changed successfully. Please sign in again.");
    }

    private Map<String, Object> authenticationResponse(Employee employee) {
        Set<String> roles = employee.getRoles().stream().map(BankingRole::name).collect(Collectors.toSet());
        Set<String> authorities = roles.stream().map(role -> "ROLE_" + role).collect(Collectors.toSet());
        String accessToken = jwtUtil.generateToken(employee.getUsername(), authorities);
        String refreshToken = jwtUtil.generateRefreshToken(employee.getUsername());
        RefreshTokenStore.save(employee.getUsername(), refreshToken);
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken); response.put("refreshToken", refreshToken);
        response.put("username", employee.getUsername()); response.put("roles", roles);
        return response;
    }
}
