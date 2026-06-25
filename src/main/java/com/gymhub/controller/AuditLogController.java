package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.response.AuditLogResponse;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * GD-14 — read-only activity history for a gym dashboard.
 */
@RestController
@RequestMapping("/api/v1/gyms/{gymId}/activity")
@RequiredArgsConstructor
@Tag(name = "Activity History", description = "Gym audit trail / activity log")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "List gym activity (optionally filter by entityType and entityId)")
    public ResponseEntity<PagedResponse<AuditLogResponse>> gymActivity(
            @PathVariable Long gymId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(
                auditLogService.getGymActivity(gymId, entityType, entityId, currentUser, pageable)));
    }
}
