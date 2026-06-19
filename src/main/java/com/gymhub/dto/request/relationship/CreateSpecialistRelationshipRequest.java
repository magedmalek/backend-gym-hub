package com.gymhub.dto.request.relationship;

import com.gymhub.domain.relationship.RelationshipType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateSpecialistRelationshipRequest {

    @NotNull(message = "Gym ID is required")
    private Long gymId;

    @NotNull(message = "Relationship type is required")
    private RelationshipType relationshipType;

    private LocalDate proposedStartDate;

    @Size(max = 2000)
    private String notes;
}
