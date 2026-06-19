package com.gymhub.domain.specialist;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "specialist_enabled_tools",
        uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "tool_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialistEnabledTool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private SpecialistProfile specialistProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_code", nullable = false)
    private ToolCode toolCode;

    @Enumerated(EnumType.STRING)
    private ServiceAxis serviceAxis;

    @Builder.Default
    private boolean enabled = true;

    /** True when this tool was auto-enabled by axis selection (not manually toggled). */
    private boolean autoEnabled;

    /** True when the specialist explicitly disabled this tool. */
    private boolean disabledBySpecialist;

    @Column(length = 500)
    private String disableReason;

    /**
     * True when the tool is part of the specialist's active service axis.
     * Disabling it triggers a frontend warning about reduced service completeness.
     */
    private boolean warningRequired;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
