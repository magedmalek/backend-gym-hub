package com.gymhub.dto.response.relationship;

import com.gymhub.domain.relationship.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class GymSpecialistRelationshipResponse {

    private Long id;

    private Long gymId;
    private String gymName;

    private Long specialistProfileId;
    private String specialistDisplayName;
    private String specialistMainSpecialization;

    private RequestInitiatorType requestInitiatorType;
    private RelationshipType relationshipType;
    private RelationshipStatus status;

    private LocalDate startDate;
    private LocalDate endDate;

    private CommercialOwner paymentOwner;
    private CommercialOwner activationOwner;
    private CommercialOwner serviceOwner;
    private CommercialOwner packageOwner;

    private boolean canSpecialistSellToGymClients;
    private boolean canSpecialistSellToNonGymClients;
    private boolean existingSubscriptionsRemainOriginalOwner;

    private boolean allowGymTrainingPatterns;
    private GymPatternPolicy gymPatternPolicy;
    private boolean mandatoryPatternsApply;
    private boolean recommendedPatternsVisible;

    private String notes;
    private LocalDateTime createdAt;
}
