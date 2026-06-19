package com.gymhub.dto.request.relationship;

import com.gymhub.domain.relationship.CommercialOwner;
import com.gymhub.domain.relationship.GymPatternPolicy;
import com.gymhub.domain.relationship.RelationshipType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateGymRelationshipRequest {

    /** The specialist profile to request a relationship with. */
    @NotNull(message = "Specialist profile ID is required")
    private Long specialistProfileId;

    @NotNull(message = "Relationship type is required")
    private RelationshipType relationshipType;

    private LocalDate proposedStartDate;

    private CommercialOwner paymentOwner;
    private CommercialOwner activationOwner;
    private CommercialOwner serviceOwner;
    private CommercialOwner packageOwner;

    private boolean canSpecialistSellToGymClients;
    private boolean canSpecialistSellToNonGymClients;

    private GymPatternPolicy gymPatternPolicy;
    private boolean mandatoryPatternsApply;
    private boolean recommendedPatternsVisible;

    @Size(max = 2000)
    private String notes;
}
