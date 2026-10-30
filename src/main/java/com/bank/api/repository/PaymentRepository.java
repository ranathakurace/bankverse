package com.bank.api.repository;
import com.bank.api.model.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentRepository extends JpaRepository<Payment, Long> { Optional<Payment> findByPaymentReference(String paymentReference); Optional<Payment> findByIdempotencyKey(String idempotencyKey); }
