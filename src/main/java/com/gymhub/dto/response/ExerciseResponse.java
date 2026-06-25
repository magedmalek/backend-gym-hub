package com.gymhub.dto.response;

import com.gymhub.domain.training.ExerciseApprovalStatus;
import com.gymhub.domain.training.ExerciseType;
import com.gymhub.domain.training.MuscleGroup;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExerciseResponse {
    private Long id;
    private String name;
    private MuscleGroup muscleGroup;
    private ExerciseType type;
    private String description;
    private String equipmentName;
    private String mediaUrl;
    private boolean global;
    private Long createdByUserId;
    private ExerciseApprovalStatus approvalStatus;
    private LocalDateTime createdAt;
}
