package com.gymhub.repository;

import com.gymhub.domain.customer.GymLinkRequest;
import com.gymhub.domain.customer.GymLinkRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GymLinkRequestRepository extends JpaRepository<GymLinkRequest, Long> {

    List<GymLinkRequest> findByUserId(Long userId);

    List<GymLinkRequest> findByGymIdAndStatus(Long gymId, GymLinkRequestStatus status);

    Optional<GymLinkRequest> findByUserIdAndGymId(Long userId, Long gymId);

    boolean existsByUserIdAndGymId(Long userId, Long gymId);
}
