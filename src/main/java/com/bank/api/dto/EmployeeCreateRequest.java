package com.bank.api.dto;
import java.util.Set;
import com.bank.api.security.BankingRole;
import lombok.Data;
@Data public class EmployeeCreateRequest { private String username; private String email; private String password; private Set<BankingRole> roles; }
