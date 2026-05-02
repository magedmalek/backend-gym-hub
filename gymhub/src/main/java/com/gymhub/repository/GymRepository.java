package com.gymhub.repository;

import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.gym.GymStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GymRepository extends JpaRepository<Gym, Long> {

    List<Gym> findByOwnerId(Long ownerId);

    List<Gym> findByStatus(GymStatus status);

    Page<Gym> findByCityIgnoreCase(String city, Pageable pageable);

    Optional<Gym> findByIdAndOwnerId(Long gymId, Long ownerId);

    boolean existsByNameIgnoreCaseAndOwnerId(String name, Long ownerId);
}
