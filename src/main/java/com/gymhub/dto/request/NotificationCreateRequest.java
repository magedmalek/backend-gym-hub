package com.gymhub.dto.request;

import com.gymhub.domain.notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request to push an in-app notification to a specific recipient user.
 * Used by dashboard operators (e.g. sending an OFFER) and by internal triggers.
 */
@Data
public class NotificationCreateRequest {

    @NotNull(message = "Recipient user id is required")
    private Long recipientUserId;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private String body;

    @Size(max = 100)
    private String referenceKey;

    private Long gymId;
}
