package com.bank.api.repository;
import com.bank.api.model.EmployeeRefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EmployeeRefreshTokenRepository extends JpaRepository<EmployeeRefreshToken, Long> { Optional<EmployeeRefreshToken> findByToken(String token); }
