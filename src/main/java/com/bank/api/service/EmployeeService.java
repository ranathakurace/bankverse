package com.bank.api.service;

import com.bank.api.dto.EmployeeCreateRequest;
import com.bank.api.dto.EmployeeResponse;
import com.bank.api.dto.EmployeeRolesRequest;
import com.bank.api.exception.EmployeeAdministrationException;
import com.bank.api.model.Employee;
import com.bank.api.repository.EmployeeRepository;
import com.bank.api.security.BankingRole;
import com.bank.api.security.PermissionService;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {
    private final EmployeeRepository repository; private final PasswordEncoder encoder; private final PermissionService permissions; private final SecurityAuditService audit;
    public EmployeeService(EmployeeRepository repository, PasswordEncoder encoder, PermissionService permissions, SecurityAuditService audit) { this.repository = repository; this.encoder = encoder; this.permissions = permissions; this.audit = audit; }
    @Transactional public EmployeeResponse create(EmployeeCreateRequest request, String actor) { validateCreate(request); String username = request.getUsername().trim().toLowerCase(Locale.ROOT); String email = request.getEmail().trim().toLowerCase(Locale.ROOT); if (repository.findByUsernameIgnoreCase(username).isPresent() || repository.findByEmailIgnoreCase(email).isPresent()) throw new EmployeeAdministrationException("Username or email already exists."); Employee e = new Employee(); e.setUsername(username); e.setEmail(email); e.setPasswordHash(encoder.encode(request.getPassword())); e.setActive(true); e.setRoles(normalizeRoles(request.getRoles())); e.setCreatedAt(LocalDateTime.now()); e.setUpdatedAt(LocalDateTime.now()); Employee saved = repository.save(e); audit.record(actor, "EMPLOYEE_CREATED", saved.getUsername(), "Roles: " + saved.getRoles()); return response(saved); }
    @Transactional public EmployeeResponse bootstrap(EmployeeCreateRequest request) { if (repository.count() > 0) throw new EmployeeAdministrationException("Initial administrator already exists."); validateCreate(request); request.setRoles(EnumSet.of(BankingRole.ADMIN)); return create(request, "SYSTEM_BOOTSTRAP"); }
    @Transactional public EmployeeResponse changeRoles(String username, EmployeeRolesRequest request, String actor) { Employee e = find(username); Set<BankingRole> roles = normalizeRoles(request == null ? null : request.getRoles()); preventLastAdminRemoval(e, roles); e.setRoles(roles); e.setUpdatedAt(LocalDateTime.now()); Employee saved = repository.save(e); audit.record(actor, "EMPLOYEE_ROLES_CHANGED", saved.getUsername(), "Roles: " + saved.getRoles()); return response(saved); }
    @Transactional public EmployeeResponse changeStatus(String username, boolean active, String actor) { Employee e = find(username); if (!active) preventLastAdminRemoval(e, e.getRoles()); e.setActive(active); e.setUpdatedAt(LocalDateTime.now()); Employee saved = repository.save(e); audit.record(actor, active ? "EMPLOYEE_ACTIVATED" : "EMPLOYEE_DEACTIVATED", saved.getUsername(), null); return response(saved); }
    @Transactional public void changePassword(String username, String currentPassword, String newPassword) { Employee employee = findActive(username); if (!encoder.matches(currentPassword, employee.getPasswordHash())) throw new EmployeeAdministrationException("Current password is incorrect."); validatePassword(newPassword); employee.setPasswordHash(encoder.encode(newPassword)); employee.setUpdatedAt(LocalDateTime.now()); repository.save(employee); audit.record(username, "PASSWORD_CHANGED", username, null); }
    @Transactional public void resetPassword(String username, String newPassword, String actor) { validatePassword(newPassword); Employee employee = find(username); employee.setPasswordHash(encoder.encode(newPassword)); employee.setUpdatedAt(LocalDateTime.now()); repository.save(employee); audit.record(actor, "PASSWORD_RESET", employee.getUsername(), null); }
    @Transactional(readOnly = true) public List<EmployeeResponse> list() { return repository.findAll().stream().map(this::response).collect(Collectors.toList()); }
    @Transactional(readOnly = true) public Page<EmployeeResponse> search(String query, Boolean active, Pageable pageable) {
        Specification<Employee> specification = (root, ignored, builder) -> builder.conjunction();
        if (!blank(query)) {
            String value = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, ignored, builder) -> builder.or(
                    builder.like(builder.lower(root.get("username")), value),
                    builder.like(builder.lower(root.get("email")), value)));
        }
        if (active != null) specification = specification.and((root, ignored, builder) -> builder.equal(root.get("active"), active));
        return repository.findAll(specification, pageable).map(this::response);
    }
    @Transactional(readOnly = true) public Employee findActive(String identity) { Employee e = repository.findByUsernameIgnoreCase(identity).orElseGet(() -> repository.findByEmailIgnoreCase(identity).orElseThrow(() -> new EmployeeAdministrationException("Employee was not found."))); if (!e.isActive()) throw new EmployeeAdministrationException("Employee account is inactive."); return e; }
    @Transactional(readOnly = true) public Employee find(String username) { return repository.findByUsernameIgnoreCase(username).orElseThrow(() -> new EmployeeAdministrationException("Employee was not found.")); }
    public EmployeeResponse response(Employee e) { EmployeeResponse r = new EmployeeResponse(); r.setUsername(e.getUsername()); r.setEmail(e.getEmail()); r.setActive(e.isActive()); r.setRoles(e.getRoles()); r.setPermissions(permissions.namesFor(e.getRoles())); return r; }
    public boolean bootstrapRequired() { return repository.count() == 0; }
    private void validateCreate(EmployeeCreateRequest r) { if (r == null || blank(r.getUsername()) || blank(r.getEmail()) || blank(r.getPassword())) throw new EmployeeAdministrationException("Username, email and password are mandatory."); if (r.getUsername().trim().length() > 100 || !r.getEmail().contains("@")) throw new EmployeeAdministrationException("Employee credentials do not meet the required format."); validatePassword(r.getPassword()); }
    private void validatePassword(String password) { if (blank(password) || password.length() < 12 || !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*[0-9].*")) throw new EmployeeAdministrationException("Password must have at least 12 characters and include upper-case, lower-case and numeric characters."); }
    private Set<BankingRole> normalizeRoles(Set<BankingRole> roles) { if (roles == null || roles.isEmpty()) throw new EmployeeAdministrationException("At least one banking role is mandatory."); return EnumSet.copyOf(roles); }
    private void preventLastAdminRemoval(Employee employee, Set<BankingRole> nextRoles) { if (employee.isActive() && employee.getRoles().contains(BankingRole.ADMIN) && (!nextRoles.contains(BankingRole.ADMIN) || !employee.isActive())) { long admins = repository.findAll().stream().filter(e -> e.isActive() && e.getRoles().contains(BankingRole.ADMIN)).count(); if (admins <= 1) throw new EmployeeAdministrationException("The final active administrator cannot be removed or deactivated."); } }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
