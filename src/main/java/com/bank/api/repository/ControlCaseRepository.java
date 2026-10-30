package com.bank.api.repository;
import com.bank.api.model.ControlCase;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ControlCaseRepository extends JpaRepository<ControlCase, Long> { Optional<ControlCase> findByCaseReference(String caseReference); }
