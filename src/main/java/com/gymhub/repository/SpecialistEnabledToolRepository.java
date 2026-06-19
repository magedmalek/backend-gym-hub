package com.gymhub.repository;

import com.gymhub.domain.specialist.SpecialistEnabledTool;
import com.gymhub.domain.specialist.ToolCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialistEnabledToolRepository extends JpaRepository<SpecialistEnabledTool, Long> {

    List<SpecialistEnabledTool> findBySpecialistProfileId(Long profileId);

    Optional<SpecialistEnabledTool> findBySpecialistProfileIdAndToolCode(Long profileId, ToolCode toolCode);

    void deleteBySpecialistProfileId(Long profileId);
}
