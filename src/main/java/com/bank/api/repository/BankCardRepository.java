package com.bank.api.repository;

import com.bank.api.model.BankCard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCardRepository extends JpaRepository<BankCard, Long> {
    Optional<BankCard> findByCardReference(String cardReference);
    List<BankCard> findByCustomer_CustomerNumberOrderByIssuedAtDesc(String customerNumber);
}
