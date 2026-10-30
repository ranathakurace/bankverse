package com.bank.api.service;
import com.bank.api.dto.SecurityAuditResponse;
import com.bank.api.model.SecurityAuditEvent;
import com.bank.api.repository.SecurityAuditEventRepository;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
@Service
public class SecurityAuditService {
    private final SecurityAuditEventRepository repository;
    public SecurityAuditService(SecurityAuditEventRepository repository) { this.repository = repository; }
    public void record(String actor, String action, String target, String details) { SecurityAuditEvent e = new SecurityAuditEvent(); e.setActorUsername(actor == null ? "SYSTEM" : actor); e.setAction(action); e.setTargetUsername(target); e.setDetails(details); e.setOccurredAt(LocalDateTime.now()); repository.save(e); }
    public List<SecurityAuditResponse> recent() { return repository.findTop100ByOrderByOccurredAtDesc().stream().map(this::response).collect(Collectors.toList()); }
    public Page<SecurityAuditResponse> search(LocalDate from, LocalDate to, String employee, String action, Pageable pageable) {
        Specification<SecurityAuditEvent> specification = (root, ignored, builder) -> builder.conjunction();
        if (from != null) specification = specification.and((root, ignored, builder) -> builder.greaterThanOrEqualTo(root.get("occurredAt"), from.atStartOfDay()));
        if (to != null) specification = specification.and((root, ignored, builder) -> builder.lessThan(root.get("occurredAt"), to.plusDays(1).atStartOfDay()));
        if (present(employee)) { String value = "%" + employee.trim().toLowerCase() + "%"; specification = specification.and((root, ignored, builder) -> builder.or(builder.like(builder.lower(root.get("actorUsername")), value), builder.like(builder.lower(root.get("targetUsername")), value))); }
        if (present(action)) { String value = "%" + action.trim().toLowerCase() + "%"; specification = specification.and((root, ignored, builder) -> builder.like(builder.lower(root.get("action")), value)); }
        return repository.findAll(specification, pageable).map(this::response);
    }
    public List<SecurityAuditResponse> export(LocalDate from, LocalDate to, String employee, String action) {
        return search(from, to, employee, action, org.springframework.data.domain.PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "occurredAt"))).getContent();
    }
    private boolean present(String value) { return value != null && !value.trim().isEmpty(); }
    private SecurityAuditResponse response(SecurityAuditEvent e) { SecurityAuditResponse r = new SecurityAuditResponse(); r.setActorUsername(e.getActorUsername()); r.setAction(e.getAction()); r.setTargetUsername(e.getTargetUsername()); r.setDetails(e.getDetails()); r.setOccurredAt(e.getOccurredAt()); return r; }
}
