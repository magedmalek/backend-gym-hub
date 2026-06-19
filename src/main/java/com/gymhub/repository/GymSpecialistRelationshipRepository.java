package com.gymhub.repository;

import com.gymhub.domain.relationship.GymSpecialistRelationship;
import com.gymhub.domain.relationship.RelationshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GymSpecialistRelationshipRepository extends JpaRepository<GymSpecialistRelationship, Long> {

    Page<GymSpecialistRelationship> findByGymId(Long gymId, Pageable pageable);

    Page<GymSpecialistRelationship> findBySpecialistProfileId(Long specialistProfileId, Pageable pageable);

    List<GymSpecialistRelationship> findByGymIdAndStatus(Long gymId, RelationshipStatus status);

    boolean existsByGymIdAndSpecialistProfileIdAndStatusIn(
            Long gymId, Long specialistProfileId, List<RelationshipStatus> statuses);
}
