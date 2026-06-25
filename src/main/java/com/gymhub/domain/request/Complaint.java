package com.gymhub.domain.request;

import com.gymhub.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A complaint raised by a customer against a gym, specialist/provider, service or session.
 * Part of FND-14 Request Center & Complaint Standards (Phase 2 plan — Section 28).
 *
 * <p>Business rules enforced by the service layer:
 * <ul>
 *   <li>A complaint must be scoped to a gym and/or a provider so it can be routed.</li>
 *   <li>Only dashboard staff of the target gym may change status / add a resolution note.</li>
 *   <li>The complainant may read and cancel (CLOSED) their own complaint while still open.</li>
 * </ul>
 */
@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_complaint_complainant", columnList = "complainant_user_id"),
        @Index(name = "idx_complaint_gym", columnList = "gym_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complainant_user_id", nullable = false)
    private User complainant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplaintType type;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(length = 2000)
    private String body;

    /** Gym the complaint is routed to (nullable for specialist-only complaints). */
    @Column(name = "gym_id")
    private Long gymId;

    /** Provider the complaint is against (nullable). */
    @Column(name = "against_provider_id")
    private Long againstProviderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.NEW;

    @Column(length = 2000)
    private String resolutionNote;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime resolvedAt;
}
