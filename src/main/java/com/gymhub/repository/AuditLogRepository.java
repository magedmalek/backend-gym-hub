package com.gymhub.repository;

import com.gymhub.domain.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByGymIdOrderByCreatedAtDesc(Long gymId, Pageable pageable);

    Page<AuditLog> findByGymIdAndEntityTypeOrderByCreatedAtDesc(
            Long gymId, String entityType, Pageable pageable);

    Page<AuditLog> findByGymIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            Long gymId, String entityType, Long entityId, Pageable pageable);
}
