package com.bank.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.api.model.KycCase;

public interface KycCaseRepository extends JpaRepository<KycCase, Long> {

    Optional<KycCase> findTopByCustomer_IdOrderByCreatedAtDesc(Long customerId);
}
