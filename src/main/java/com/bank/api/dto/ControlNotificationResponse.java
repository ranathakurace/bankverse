package com.bank.api.dto;
import lombok.Data;
@Data public class ControlNotificationResponse { private String notificationReference; private String recipient; private String channel; private String status; private String relatedReference; }
