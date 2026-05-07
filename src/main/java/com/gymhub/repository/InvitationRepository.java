package com.gymhub.repository;

import com.gymhub.domain.invitation.Invitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Page<Invitation> findByHostId(Long hostCustomerId, Pageable pageable);

    Page<Invitation> findByHostIdIn(java.util.List<Long> hostCustomerIds, Pageable pageable);

    Page<Invitation> findByGuestUserId(Long guestUserId, Pageable pageable);

    Page<Invitation> findByGymId(Long gymId, Pageable pageable);

    long countBySubscriptionId(Long subscriptionId);

    /** Check whether the same guest has already visited under this subscription. */
    boolean existsBySubscriptionIdAndGuestUserId(Long subscriptionId, Long guestUserId);
}
