package com.gymhub.repository;

import com.gymhub.domain.family.FamilyMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyMembershipRepository extends JpaRepository<FamilyMembership, Long> {

    List<FamilyMembership> findBySubscriptionId(Long subscriptionId);

    long countBySubscriptionId(Long subscriptionId);

    boolean existsBySubscriptionIdAndSubCustomerId(Long subscriptionId, Long subCustomerId);

    void deleteBySubscriptionIdAndSubCustomerId(Long subscriptionId, Long subCustomerId);
}
