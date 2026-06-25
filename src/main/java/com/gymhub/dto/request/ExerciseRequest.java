package com.gymhub.dto.request;

import com.gymhub.domain.training.ExerciseType;
import com.gymhub.domain.training.MuscleGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Create a custom library exercise. POST /api/v1/specialists/me/exercises.
 */
@Data
public class ExerciseRequest {

    @NotBlank(message = "Exercise name is required")
    @Size(max = 200)
    private String name;

    @NotNull(message = "Muscle group is required")
    private MuscleGroup muscleGroup;

    @NotNull(message = "Exercise type is required")
    private ExerciseType type;

    @Size(max = 2000)
    private String description;

    @Size(max = 200)
    private String equipmentName;

    @Size(max = 500)
    private String mediaUrl;
}
