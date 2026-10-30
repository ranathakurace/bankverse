package com.bank.api.repository;
import com.bank.api.model.Employee;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> { Optional<Employee> findByUsernameIgnoreCase(String username); Optional<Employee> findByEmailIgnoreCase(String email); long countByActiveTrueAndRolesContains(com.bank.api.security.BankingRole role); }
