package com.gymhub.service;

import com.gymhub.domain.notification.Notification;
import com.gymhub.domain.notification.NotificationChannel;
import com.gymhub.domain.notification.NotificationType;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.NotificationCreateRequest;
import com.gymhub.dto.response.NotificationResponse;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.repository.NotificationRepository;
import com.gymhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FND-05 Notifications Engine.
 *
 * <p>Exposes two surfaces:
 * <ul>
 *   <li>A programmatic {@link #notify} API other services can call to emit an in-app
 *       notification when a business event happens.</li>
 *   <li>A recipient-facing API (list / unread-count / mark-read) used by GD-13 and CA-11.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ── Emit ────────────────────────────────────────────────────────────────

    /**
     * Programmatic entry point for internal triggers. Persists an IN_APP notification.
     */
    @Transactional
    public Notification notify(User recipient, NotificationType type, String title,
                               String body, String referenceKey, Long gymId) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .channel(NotificationChannel.IN_APP)
                .title(title)
                .body(body)
                .referenceKey(referenceKey)
                .gymId(gymId)
                .build();
        return notificationRepository.save(notification);
    }

    /**
     * Operator-driven push (e.g. an OFFER sent to a member) addressed by recipient user id.
     */
    @Transactional
    public NotificationResponse push(NotificationCreateRequest request) {
        User recipient = userRepository.findById(request.getRecipientUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getRecipientUserId()));
        Notification saved = notify(recipient, request.getType(), request.getTitle(),
                request.getBody(), request.getReferenceKey(), request.getGymId());
        return toResponse(saved);
    }

    // ── Recipient queries ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(User currentUser, boolean unreadOnly,
                                                         Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(
                        currentUser.getId(), pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                        currentUser.getId(), pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User currentUser) {
        return notificationRepository.countByRecipientIdAndReadFalse(currentUser.getId());
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, User currentUser) {
        Notification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        notification.markRead();
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public long markAllRead(User currentUser) {
        Page<Notification> unread = notificationRepository
                .findByRecipientIdAndReadFalseOrderByCreatedAtDesc(
                        currentUser.getId(), Pageable.unpaged());
        unread.forEach(Notification::markRead);
        notificationRepository.saveAll(unread);
        return unread.getTotalElements();
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .channel(n.getChannel())
                .title(n.getTitle())
                .body(n.getBody())
                .referenceKey(n.getReferenceKey())
                .gymId(n.getGymId())
                .read(n.isRead())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
