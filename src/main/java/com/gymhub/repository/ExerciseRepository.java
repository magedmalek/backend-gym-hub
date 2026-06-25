package com.gymhub.repository;

import com.gymhub.domain.training.Exercise;
import com.gymhub.domain.training.ExerciseApprovalStatus;
import com.gymhub.domain.training.ExerciseType;
import com.gymhub.domain.training.MuscleGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    @Query("SELECT e FROM Exercise e WHERE e.approvalStatus = :status " +
            "AND (:muscleGroup IS NULL OR e.muscleGroup = :muscleGroup) " +
            "AND (:type IS NULL OR e.type = :type) " +
            "AND (:q IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Exercise> search(@Param("status") ExerciseApprovalStatus status,
                          @Param("muscleGroup") MuscleGroup muscleGroup,
                          @Param("type") ExerciseType type,
                          @Param("q") String q,
                          Pageable pageable);

    Page<Exercise> findByCreatedByIdOrderByCreatedAtDesc(Long createdById, Pageable pageable);
}
