package com.gymhub.repository;

import com.gymhub.domain.gymservice.GymService;
import com.gymhub.domain.gymservice.ServiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GymServiceRepository extends JpaRepository<GymService, Long> {

    Page<GymService> findByGymId(Long gymId, Pageable pageable);

    List<GymService> findByGymIdAndStatus(Long gymId, ServiceStatus status);

    List<GymService> findByGymIdAndCanBeIncludedInPackageTrue(Long gymId);

    List<GymService> findByGymIdAndCanBeSoldIndependentlyTrue(Long gymId);

    boolean existsByNameIgnoreCaseAndGymId(String name, Long gymId);
}
