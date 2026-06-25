package com.gymhub.service;

import com.gymhub.domain.audit.AuditLog;
import com.gymhub.domain.user.User;
import com.gymhub.dto.response.AuditLogResponse;
import com.gymhub.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GD-14 Activity History / Audit Log (Phase 2 plan — Section 29).
 *
 * <p>{@link #record} is the append-only programmatic entry point other services call after
 * a sensitive operation. Reads are restricted to gym dashboard users via {@link GymAccessService}.</p>
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final GymAccessService gymAccessService;

    // ── Record ────────────────────────────────────────────────────────────────

    @Transactional
    public AuditLog record(Long gymId, User actor, String action,
                           String entityType, Long entityId, String description) {
        AuditLog log = AuditLog.builder()
                .gymId(gymId)
                .actor(actor)
                .actorName(actor != null ? actor.getFullName() : "system")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .build();
        return auditLogRepository.save(log);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getGymActivity(Long gymId, String entityType, Long entityId,
                                                 User currentUser, Pageable pageable) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);

        Page<AuditLog> page;
        if (entityType != null && entityId != null) {
            page = auditLogRepository.findByGymIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    gymId, entityType, entityId, pageable);
        } else if (entityType != null) {
            page = auditLogRepository.findByGymIdAndEntityTypeOrderByCreatedAtDesc(
                    gymId, entityType, pageable);
        } else {
            page = auditLogRepository.findByGymIdOrderByCreatedAtDesc(gymId, pageable);
        }
        return page.map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .gymId(a.getGymId())
                .actorUserId(a.getActor() != null ? a.getActor().getId() : null)
                .actorName(a.getActorName())
                .action(a.getAction())
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .description(a.getDescription())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
