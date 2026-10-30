package com.bank.api.model;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @Entity @Table(name = "control_notifications")
public class ControlNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "notification_reference", nullable = false, unique = true, length = 50) private String notificationReference;
    @Column(nullable = false, length = 100) private String recipient;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private NotificationChannel channel;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private NotificationStatus status;
    @Column(nullable = false, length = 500) private String message;
    @Column(name = "related_reference", nullable = false, length = 100) private String relatedReference;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "sent_at") private LocalDateTime sentAt;
}
