package com.gymhub.service;

import com.gymhub.domain.training.Exercise;
import com.gymhub.domain.training.ExerciseApprovalStatus;
import com.gymhub.domain.training.ExerciseType;
import com.gymhub.domain.training.MuscleGroup;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.ExerciseRequest;
import com.gymhub.dto.response.ExerciseResponse;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 2 plan — Section 11 (Exercise Library). Public browsing of APPROVED exercises and
 * provider-side creation of custom exercises (starting PENDING_APPROVAL).
 */
@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    // ── Public browse ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ExerciseResponse> browse(MuscleGroup muscleGroup, ExerciseType type,
                                         String q, Pageable pageable) {
        return exerciseRepository
                .search(ExerciseApprovalStatus.APPROVED, muscleGroup, type, q, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ExerciseResponse getOne(Long exerciseId) {
        return toResponse(requireExercise(exerciseId));
    }

    // ── Provider creates a custom exercise ─────────────────────────────────────

    @Transactional
    public ExerciseResponse createCustom(ExerciseRequest request, User currentUser) {
        Exercise exercise = Exercise.builder()
                .name(request.getName())
                .muscleGroup(request.getMuscleGroup())
                .type(request.getType())
                .description(request.getDescription())
                .equipmentName(request.getEquipmentName())
                .mediaUrl(request.getMediaUrl())
                .global(false)
                .createdBy(currentUser)
                .approvalStatus(ExerciseApprovalStatus.PENDING_APPROVAL)
                .build();
        return toResponse(exerciseRepository.save(exercise));
    }

    @Transactional(readOnly = true)
    public Page<ExerciseResponse> getMyExercises(User currentUser, Pageable pageable) {
        return exerciseRepository.findByCreatedByIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(this::toResponse);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Exercise requireExercise(Long id) {
        return exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", id));
    }

    private ExerciseResponse toResponse(Exercise e) {
        return ExerciseResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .muscleGroup(e.getMuscleGroup())
                .type(e.getType())
                .description(e.getDescription())
                .equipmentName(e.getEquipmentName())
                .mediaUrl(e.getMediaUrl())
                .global(e.isGlobal())
                .createdByUserId(e.getCreatedBy() != null ? e.getCreatedBy().getId() : null)
                .approvalStatus(e.getApprovalStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
