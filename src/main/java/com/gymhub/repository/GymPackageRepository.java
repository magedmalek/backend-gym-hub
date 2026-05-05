package com.gymhub.repository;

import com.gymhub.domain.gympackage.GymPackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GymPackageRepository extends JpaRepository<GymPackage, Long> {

    Page<GymPackage> findByGymId(Long gymId, Pageable pageable);

    List<GymPackage> findByGymIdAndActive(Long gymId, boolean active);
}
