package com.gymhub.repository;

import com.gymhub.domain.specialist.SpecialistProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecialistProfileRepository extends JpaRepository<SpecialistProfile, Long> {

    Optional<SpecialistProfile> findByUserId(Long userId);

    Optional<SpecialistProfile> findByProviderId(Long providerId);

    boolean existsByUserId(Long userId);
}
