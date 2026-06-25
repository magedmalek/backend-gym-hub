package com.gymhub.repository;

import com.gymhub.domain.request.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Page<Complaint> findByComplainantIdOrderByCreatedAtDesc(Long complainantId, Pageable pageable);

    Page<Complaint> findByGymIdOrderByCreatedAtDesc(Long gymId, Pageable pageable);

    Optional<Complaint> findByIdAndComplainantId(Long id, Long complainantId);

    Optional<Complaint> findByIdAndGymId(Long id, Long gymId);
}
