package com.bank.api.dto;
import java.util.Set;
import com.bank.api.security.BankingRole;
import lombok.Data;
@Data public class EmployeeResponse { private String username; private String email; private boolean active; private Set<BankingRole> roles; private Set<String> permissions; }
