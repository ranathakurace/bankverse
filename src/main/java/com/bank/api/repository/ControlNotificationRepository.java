package com.bank.api.repository;
import com.bank.api.model.ControlNotification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ControlNotificationRepository extends JpaRepository<ControlNotification, Long> { Optional<ControlNotification> findByNotificationReference(String notificationReference); }
