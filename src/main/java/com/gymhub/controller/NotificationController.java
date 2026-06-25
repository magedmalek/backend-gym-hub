package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.request.NotificationCreateRequest;
import com.gymhub.dto.response.NotificationResponse;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * FND-05 / GD-13 / CA-11 — in-app notification center for the authenticated user.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification center (list, unread count, mark read)")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List my notifications (newest first); set unreadOnly=true for the unread feed")
    public ResponseEntity<PagedResponse<NotificationResponse>> myNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(
                notificationService.getMyNotifications(currentUser, unreadOnly, pageable)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Number of unread notifications for the badge counter")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(currentUser)));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(notificationService.markRead(notificationId, currentUser));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all my notifications as read")
    public ResponseEntity<Map<String, Long>> markAllRead(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(Map.of("markedRead", notificationService.markAllRead(currentUser)));
    }

    @PostMapping
    @Operation(summary = "Send an in-app notification to a user (e.g. an OFFER from the dashboard)")
    public ResponseEntity<NotificationResponse> push(
            @Valid @RequestBody NotificationCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.push(request));
    }
}
