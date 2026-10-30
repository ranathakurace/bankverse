package com.bank.api.controller;

import com.bank.api.dto.EmployeeCreateRequest;
import com.bank.api.dto.EmployeeResponse;
import com.bank.api.dto.EmployeeRolesRequest;
import com.bank.api.dto.PasswordResetRequest;
import com.bank.api.dto.SecurityAuditResponse;
import com.bank.api.service.EmployeeService;
import com.bank.api.service.SecurityAuditService;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Administrative identity lifecycle APIs. Every operation is restricted to ADMIN. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeAdministrationController {
    private final EmployeeService employees;
    private final SecurityAuditService audit;

    public EmployeeAdministrationController(EmployeeService employees, SecurityAuditService audit) {
        this.employees = employees;
        this.audit = audit;
    }

    @GetMapping("/employees")
    public Page<EmployeeResponse> listEmployees(@RequestParam(required = false) String query, @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return employees.search(query, active, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by("username").ascending()));
    }

    @PostMapping("/employees")
    public ResponseEntity<EmployeeResponse> createEmployee(@RequestBody EmployeeCreateRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employees.create(request, principal.getName()));
    }

    @PutMapping("/employees/{username}/roles")
    public EmployeeResponse assignRoles(@PathVariable String username, @RequestBody EmployeeRolesRequest request, Principal principal) {
        return employees.changeRoles(username, request, principal.getName());
    }

    @PutMapping("/employees/{username}/status")
    public EmployeeResponse setStatus(@PathVariable String username, @RequestParam boolean active, Principal principal) {
        return employees.changeStatus(username, active, principal.getName());
    }

    @PutMapping("/employees/{username}/password")
    public java.util.Map<String, String> resetPassword(@PathVariable String username, @RequestBody PasswordResetRequest request, Principal principal) {
        employees.resetPassword(username, request.getNewPassword(), principal.getName());
        return java.util.Collections.singletonMap("message", "Password reset successfully.");
    }

    @GetMapping("/security-audit")
    public Page<SecurityAuditResponse> recentAudit(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String employee, @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return audit.search(from, to, employee, action, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(Sort.Direction.DESC, "occurredAt")));
    }

    @GetMapping(value = "/security-audit/export", produces = "text/csv")
    public ResponseEntity<String> exportAudit(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String employee, @RequestParam(required = false) String action) {
        StringBuilder csv = new StringBuilder("occurredAt,action,actorUsername,targetUsername,details\n");
        for (SecurityAuditResponse event : audit.export(from, to, employee, action)) csv.append(csv(event.getOccurredAt())).append(',').append(csv(event.getAction())).append(',').append(csv(event.getActorUsername())).append(',').append(csv(event.getTargetUsername())).append(',').append(csv(event.getDetails())).append('\n');
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=security-audit.csv").contentType(MediaType.parseMediaType("text/csv")).body(csv.toString());
    }
    private String csv(Object value) { return "\"" + (value == null ? "" : value.toString().replace("\"", "\"\"")) + "\""; }
}
