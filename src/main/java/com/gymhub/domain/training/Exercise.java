package com.gymhub.domain.training;

import com.gymhub.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A reusable exercise in the training library (Phase 2 plan — Section 11, Exercise Library;
 * foundation for GD-11 / PV-04 / CA-08).
 *
 * <p>Global exercises ({@code global = true}) are platform-curated and APPROVED. Provider-created
 * exercises are owned by the creating user and start as PENDING_APPROVAL.</p>
 */
@Entity
@Table(name = "exercises", indexes = {
        @Index(name = "idx_exercise_muscle", columnList = "muscle_group"),
        @Index(name = "idx_exercise_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MuscleGroup muscleGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExerciseType type;

    @Column(length = 2000)
    private String description;

    @Column(length = 200)
    private String equipmentName;

    @Column(length = 500)
    private String mediaUrl;

    /** Platform-curated library exercise available to everyone. */
    @Column(nullable = false)
    @Builder.Default
    private boolean global = false;

    /** Creator (null for platform/global exercises). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ExerciseApprovalStatus approvalStatus = ExerciseApprovalStatus.APPROVED;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
