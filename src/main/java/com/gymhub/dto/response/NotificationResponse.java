package com.gymhub.dto.response;

import com.gymhub.domain.notification.NotificationChannel;
import com.gymhub.domain.notification.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private NotificationChannel channel;
    private String title;
    private String body;
    private String referenceKey;
    private Long gymId;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
