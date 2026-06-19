package com.gymhub.dto.response.specialist;

import com.gymhub.domain.specialist.SpecialistSpecialization;
import com.gymhub.domain.specialist.SpecialistStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SpecialistProfileResponse {

    private Long id;
    private Long providerId;
    private Long userId;
    private String displayName;
    private SpecialistSpecialization mainSpecialization;
    private List<SpecialistSpecialization> additionalSpecializations;
    private String bio;
    private int experienceYears;
    private String profileImageUrl;
    private String coverImageUrl;
    private boolean worksIndependently;
    private SpecialistStatus status;
    private boolean active;
    private LocalDateTime createdAt;
}
