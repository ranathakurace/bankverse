package com.bank.api.security;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
@Service
public class PermissionService {
    private final Map<BankingRole, Set<BankingPermission>> matrix = new EnumMap<>(BankingRole.class);
    public PermissionService() {
        matrix.put(BankingRole.CUSTOMER_OFFICER, EnumSet.of(BankingPermission.CUSTOMER_CREATE, BankingPermission.CUSTOMER_READ, BankingPermission.CUSTOMER_UPDATE));
        matrix.put(BankingRole.KYC_OFFICER, EnumSet.of(BankingPermission.CUSTOMER_READ, BankingPermission.KYC_READ, BankingPermission.KYC_REVIEW, BankingPermission.KYC_APPROVE));
        matrix.put(BankingRole.ACCOUNT_OFFICER, EnumSet.of(BankingPermission.CUSTOMER_READ, BankingPermission.ACCOUNT_READ, BankingPermission.ACCOUNT_CREATE, BankingPermission.ACCOUNT_UPDATE, BankingPermission.PRODUCT_MANAGE));
        matrix.put(BankingRole.OPERATIONS_MANAGER, EnumSet.of(BankingPermission.CUSTOMER_READ, BankingPermission.ACCOUNT_READ, BankingPermission.TRANSACTION_READ, BankingPermission.TRANSACTION_INITIATE, BankingPermission.TRANSACTION_APPROVE, BankingPermission.CONTROL_REVIEW, BankingPermission.EVENT_OPERATE));
        matrix.put(BankingRole.AUDITOR, EnumSet.of(BankingPermission.CUSTOMER_READ, BankingPermission.KYC_READ, BankingPermission.ACCOUNT_READ, BankingPermission.TRANSACTION_READ, BankingPermission.AUDIT_READ));
        matrix.put(BankingRole.ADMIN, EnumSet.of(BankingPermission.USER_MANAGE, BankingPermission.ROLE_MANAGE, BankingPermission.AUDIT_READ));
    }
    public Set<BankingPermission> permissionsFor(Set<BankingRole> roles) { Set<BankingPermission> result = EnumSet.noneOf(BankingPermission.class); if (roles != null) roles.forEach(role -> result.addAll(matrix.getOrDefault(role, Collections.emptySet()))); return result; }
    public Set<String> namesFor(Set<BankingRole> roles) { Set<String> names = new LinkedHashSet<>(); permissionsFor(roles).forEach(permission -> names.add(permission.name())); return names; }
}
