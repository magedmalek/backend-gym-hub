package com.gymhub.repository;

import com.gymhub.domain.specialist.ServiceAxis;
import com.gymhub.domain.specialist.SpecialistServiceAxisSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialistServiceAxisSelectionRepository extends JpaRepository<SpecialistServiceAxisSelection, Long> {

    List<SpecialistServiceAxisSelection> findBySpecialistProfileId(Long profileId);

    Optional<SpecialistServiceAxisSelection> findBySpecialistProfileIdAndServiceAxis(Long profileId, ServiceAxis axis);

    void deleteBySpecialistProfileId(Long profileId);
}
