package com.bank.api.controller;

import com.bank.api.dto.CustomerOnlineAccessRequest;
import com.bank.api.dto.CustomerOnlineLoginRequest;
import com.bank.api.dto.PasswordChangeRequest;
import com.bank.api.exception.AuthenticationException;
import com.bank.api.exception.CustomerPortalException;
import com.bank.api.model.Customer;
import com.bank.api.security.JwtUtil;
import com.bank.api.security.RefreshTokenStore;
import com.bank.api.service.CustomerPortalService;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Public customer authentication, deliberately separate from staff authentication. */
@RestController
public class CustomerPortalAuthController {
    private final CustomerPortalService portal;
    private final JwtUtil jwtUtil;
    public CustomerPortalAuthController(CustomerPortalService portal, JwtUtil jwtUtil) { this.portal = portal; this.jwtUtil = jwtUtil; }
    @PostMapping("/customer-auth/enroll") public Map<String, Object> enroll(@RequestBody CustomerOnlineAccessRequest request) { return response(portal.enroll(request)); }
    @PostMapping("/customer-auth/login") public Map<String, Object> login(@RequestBody CustomerOnlineLoginRequest request) { try { return response(portal.authenticate(request)); } catch (CustomerPortalException exception) { throw new AuthenticationException("Invalid customer credentials or online access is unavailable."); } }
    @PostMapping("/customer-auth/password") public Map<String, String> password(@RequestBody PasswordChangeRequest request, Principal principal) { portal.changePassword(principal.getName(), request); return Collections.singletonMap("message", "Password changed successfully. Please sign in again."); }
    private Map<String, Object> response(Customer customer) { String subject = customer.getCustomerNumber(); String accessToken = jwtUtil.generateToken(subject, Collections.singleton("ROLE_CUSTOMER")); String refreshToken = jwtUtil.generateRefreshToken(subject); RefreshTokenStore.save(subject, refreshToken); Map<String, Object> response = new HashMap<>(); response.put("accessToken", accessToken); response.put("refreshToken", refreshToken); response.put("username", subject); response.put("displayName", customer.getFullName()); response.put("roles", Collections.singleton("CUSTOMER")); return response; }
}
