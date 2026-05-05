package com.gymhub.repository;

import com.gymhub.domain.extraservice.ServiceUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceUsageRepository extends JpaRepository<ServiceUsage, Long> {

    Page<ServiceUsage> findByCustomerIdOrderByUsedAtDesc(Long customerId, Pageable pageable);

    Page<ServiceUsage> findBySubscriptionIdOrderByUsedAtDesc(Long subscriptionId, Pageable pageable);

    long countBySubscriptionIdAndServiceId(Long subscriptionId, Long serviceId);
}
