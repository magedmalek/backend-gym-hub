package com.gymhub.dto.request.relationship;

import com.gymhub.domain.relationship.CommercialOwner;
import com.gymhub.domain.relationship.GymPatternPolicy;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateRelationshipPolicyRequest {

    private CommercialOwner paymentOwner;
    private CommercialOwner activationOwner;
    private CommercialOwner serviceOwner;
    private CommercialOwner packageOwner;

    private Boolean canSpecialistSellToGymClients;
    private Boolean canSpecialistSellToNonGymClients;

    private GymPatternPolicy gymPatternPolicy;
    private Boolean mandatoryPatternsApply;
    private Boolean recommendedPatternsVisible;

    @Size(max = 2000)
    private String notes;
}
