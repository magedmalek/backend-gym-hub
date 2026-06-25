package com.gymhub.controller;

import com.gymhub.domain.training.ExerciseType;
import com.gymhub.domain.training.MuscleGroup;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.ExerciseRequest;
import com.gymhub.dto.response.ExerciseResponse;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.service.ExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Phase 2 plan — Section 11 (Exercise Library).
 * Public browsing under /api/v1/exercises (whitelisted in SecurityConfig);
 * authenticated creation under /api/v1/specialists/me/exercises.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Exercise Library", description = "Browse and create training exercises (GD-11/PV-04/CA-08)")
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping("/api/v1/exercises")
    @Operation(summary = "Browse approved exercises (filter by muscleGroup, type, name)")
    public ResponseEntity<PagedResponse<ExerciseResponse>> browse(
            @RequestParam(required = false) MuscleGroup muscleGroup,
            @RequestParam(required = false) ExerciseType type,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(
                exerciseService.browse(muscleGroup, type, q, pageable)));
    }

    @GetMapping("/api/v1/exercises/{exerciseId}")
    @Operation(summary = "Get a single exercise")
    public ResponseEntity<ExerciseResponse> getOne(@PathVariable Long exerciseId) {
        return ResponseEntity.ok(exerciseService.getOne(exerciseId));
    }

    @PostMapping("/api/v1/specialists/me/exercises")
    @Operation(summary = "Create a custom exercise (starts as PENDING_APPROVAL)")
    public ResponseEntity<ExerciseResponse> createCustom(
            @Valid @RequestBody ExerciseRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exerciseService.createCustom(request, currentUser));
    }

    @GetMapping("/api/v1/specialists/me/exercises")
    @Operation(summary = "List exercises I created")
    public ResponseEntity<PagedResponse<ExerciseResponse>> myExercises(
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(
                exerciseService.getMyExercises(currentUser, pageable)));
    }
}
