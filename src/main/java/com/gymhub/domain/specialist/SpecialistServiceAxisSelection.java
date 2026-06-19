package com.gymhub.domain.specialist;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "specialist_service_axis_selections",
        uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "service_axis"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialistServiceAxisSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private SpecialistProfile specialistProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceAxis serviceAxis;

    private boolean isMainAxis;

    @Builder.Default
    private boolean active = true;

    private LocalDateTime selectedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
