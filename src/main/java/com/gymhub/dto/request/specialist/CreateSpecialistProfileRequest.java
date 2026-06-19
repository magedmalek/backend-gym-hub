package com.gymhub.dto.request.specialist;

import com.gymhub.domain.specialist.SpecialistSpecialization;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateSpecialistProfileRequest {

    @NotNull(message = "Main specialization is required")
    private SpecialistSpecialization mainSpecialization;

    private List<SpecialistSpecialization> additionalSpecializations;

    @Size(max = 2000)
    private String bio;

    @Min(0) @Max(60)
    private int experienceYears;

    @Size(max = 500)
    private String profileImageUrl;

    @Size(max = 500)
    private String coverImageUrl;

    private boolean worksIndependently;

    /** Display name for the auto-created specialist provider. */
    @Size(max = 200)
    private String displayName;
}
