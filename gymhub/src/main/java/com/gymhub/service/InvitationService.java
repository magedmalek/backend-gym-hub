package com.gymhub.service;

import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.domain.invitation.Invitation;
import com.gymhub.domain.subscription.Subscription;
import com.gymhub.domain.subscription.SubscriptionStatus;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.UseInvitationRequest;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.repository.InvitationRepository;
import com.gymhub.repository.SubscriptionRepository;
import com.gymhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final CustomerService customerService;
    private final GymAccessService gymAccessService;

    @Transactional
    public Invitation useInvitation(Long gymId, UseInvitationRequest request, User currentUser) {
        Employee emp = gymAccessService.resolveActingEmployee(
                currentUser, gymId, EmployeePermission.REGISTER_INVITATION);

        Customer host = customerService.findOrThrow(request.getHostCustomerId());
        Subscription sub = findActiveSubscription(request.getSubscriptionId(), gymId);

        // Check invitation quota
        if (sub.getRemainingInvitations() <= 0) {
            throw new BusinessException("No invitation slots remaining on this subscription");
        }

        // Resolve or create guest user
        User guestUser;
        boolean isNewAccount = false;

        if (request.getExistingGuestUserId() != null) {
            guestUser = userRepository.findById(request.getExistingGuestUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Guest user", request.getExistingGuestUserId()));
        } else {
            // Check if guest already brought under this subscription (if repeat visit not allowed)
            if (!sub.getGymPackage().isAllowGuestRepeatVisit() && request.getGuestEmail() != null) {
                userRepository.findByEmail(request.getGuestEmail()).ifPresent(existingUser -> {
                    if (invitationRepository.existsBySubscriptionIdAndGuestUserId(sub.getId(), existingUser.getId())) {
                        throw new BusinessException("This guest has already used an invitation on this subscription");
                    }
                });
            }
            guestUser = customerService.createGuestUser(
                    request.getGuestFirstName(),
                    request.getGuestLastName(),
                    request.getGuestEmail(),
                    request.getGuestPhone()
            );
            isNewAccount = true;
        }

        // Check repeat visit rule for existing users
        if (!sub.getGymPackage().isAllowGuestRepeatVisit() && !isNewAccount) {
            if (invitationRepository.existsBySubscriptionIdAndGuestUserId(sub.getId(), guestUser.getId())) {
                throw new BusinessException("Guest repeat visit is not allowed for this package");
            }
        }

        // Consume one invitation slot
        sub.setUsedInvitations(sub.getUsedInvitations() + 1);
        subscriptionRepository.save(sub);

        Invitation invitation = Invitation.builder()
                .host(host)
                .subscription(sub)
                .guestUser(guestUser)
                .gym(host.getGym())
                .recordedBy(emp)
                .newGuestAccount(isNewAccount)
                .guestRepeatVisitAllowed(sub.getGymPackage().isAllowGuestRepeatVisit())
                .build();

        return invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public Page<Invitation> getByHost(Long gymId, Long hostCustomerId,
                                       User currentUser, Pageable pageable) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        return invitationRepository.findByHostId(hostCustomerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Invitation> getByGym(Long gymId, User currentUser, Pageable pageable) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        return invitationRepository.findByGymId(gymId, pageable);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Subscription findActiveSubscription(Long subscriptionId, Long gymId) {
        Subscription sub = subscriptionRepository.findByIdAndGymId(subscriptionId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", subscriptionId));
        if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessException("Subscription is not active");
        }
        return sub;
    }
}
