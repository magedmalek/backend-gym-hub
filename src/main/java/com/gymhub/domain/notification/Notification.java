package com.gymhub.domain.notification;

import com.gymhub.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A single in-app notification addressed to a recipient {@link User}.
 *
 * <p>FND-05 Notifications Engine. Notifications are always stored for the IN_APP channel
 * (read/unread tracking). External channels (PUSH/SMS/EMAIL) reuse the same record as the
 * delivery audit anchor.</p>
 *
 * <p>Business rules enforced by the service layer:
 * <ul>
 *   <li>A notification belongs to exactly one recipient user.</li>
 *   <li>A recipient can only read / mark their own notifications.</li>
 *   <li>Marking read is idempotent and stamps {@code readAt} once.</li>
 * </ul>
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_recipient", columnList = "recipient_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private NotificationChannel channel = NotificationChannel.IN_APP;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String body;

    /** Optional deep-link reference, e.g. "subscription:42" or "gym:7". */
    @Column(length = 100)
    private String referenceKey;

    /** Optional gym scope so dashboards can filter notifications per gym. */
    @Column(name = "gym_id")
    private Long gymId;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    private LocalDateTime readAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void markRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }
}
