package com.gymhub.domain.gymservice;

import com.gymhub.domain.gym.Gym;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A service or class offered by a gym, with a freely-chosen name.
 * The gym defines all naming — the system imposes no fixed taxonomy.
 *
 * Examples: "يوجا", "كلاس يوجا", "سيشن يوجا", "Spinning", "PT Session"
 *
 * A service can be:
 *  - embedded inside a subscription package (includedInPackage = true)
 *  - sold as an independent paid add-on (soldIndependently = true)
 *  - both, or neither (e.g. informational only)
 */
@Entity
@Table(name = "gym_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GymService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    /** Freely-chosen name written by the gym. */
    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    /** Can this service be included in a subscription package? */
    @Builder.Default
    private boolean canBeIncludedInPackage = true;

    /** Can this service be sold as a standalone paid add-on? */
    @Builder.Default
    private boolean canBeSoldIndependently = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.ACTIVE;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
