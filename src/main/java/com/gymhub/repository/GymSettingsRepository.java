package com.gymhub.repository;

import com.gymhub.domain.gym.GymSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GymSettingsRepository extends JpaRepository<GymSettings, Long> {

    Optional<GymSettings> findByGymId(Long gymId);
}
