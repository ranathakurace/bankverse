package com.bank.api.dto;
import java.util.Set;
import com.bank.api.security.BankingRole;
import lombok.Data;
@Data public class EmployeeRolesRequest { private Set<BankingRole> roles; }
