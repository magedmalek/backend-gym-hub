package com.gymhub.domain.audit;

import com.gymhub.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Immutable activity / audit record (GD-14 Activity History; Phase 2 plan — Section 29).
 *
 * <p>Captures who did what, to which entity, in which gym scope. Records are append-only;
 * the service layer never updates or deletes them.</p>
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_gym", columnList = "gym_id"),
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Gym scope of the action; null for platform-/account-level events. */
    @Column(name = "gym_id")
    private Long gymId;

    /** The user who performed the action. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    /** Denormalised actor display name so history survives user changes. */
    @Column(length = 200)
    private String actorName;

    /** Machine action code, e.g. "SUBSCRIPTION_ACTIVATED", "EMPLOYEE_DEACTIVATED". */
    @Column(name = "action", nullable = false, length = 80)
    private String action;

    /** Logical entity the action targeted, e.g. "Subscription", "Customer". */
    @Column(name = "entity_type", length = 60)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    /** Human-readable summary of the change. */
    @Column(length = 1000)
    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
