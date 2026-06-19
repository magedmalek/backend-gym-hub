package com.gymhub.domain.specialist;

import com.gymhub.domain.provider.Provider;
import com.gymhub.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "specialist_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialistProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false, unique = true)
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpecialistSpecialization mainSpecialization;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "specialist_additional_specializations",
            joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "specialization")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private List<SpecialistSpecialization> additionalSpecializations = new ArrayList<>();

    @Column(length = 2000)
    private String bio;

    private int experienceYears;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(length = 500)
    private String coverImageUrl;

    private boolean worksIndependently;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SpecialistStatus status = SpecialistStatus.DRAFT;

    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
