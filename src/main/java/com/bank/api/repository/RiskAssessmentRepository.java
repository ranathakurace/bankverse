package com.bank.api.repository;
import com.bank.api.model.RiskAssessment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> { Optional<RiskAssessment> findByAssessmentReference(String assessmentReference); }
