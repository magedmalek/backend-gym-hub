package com.gymhub.service;

import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.domain.family.FamilyMembership;
import com.gymhub.domain.subscription.Subscription;
import com.gymhub.domain.subscription.SubscriptionStatus;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.AddSubUserRequest;
import com.gymhub.dto.response.FamilyMemberResponse;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.DuplicateResourceException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.repository.FamilyMembershipRepository;
import com.gymhub.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FamilyMembershipService {

    private final FamilyMembershipRepository membershipRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CustomerService customerService;
    private final GymAccessService gymAccessService;

    @Transactional
    public FamilyMemberResponse addSubUser(Long gymId, Long subscriptionId,
                                            AddSubUserRequest request, User currentUser) {
        Employee emp = gymAccessService.resolveActingEmployee(
                currentUser, gymId, EmployeePermission.MANAGE_CUSTOMERS);

        Subscription sub = findActiveSubscription(subscriptionId, gymId);

        if (!sub.getGymPackage().isFamilyPackage()) {
            throw new BusinessException("This subscription's package is not a family package");
        }

        long currentSubUsers = membershipRepository.countBySubscriptionId(subscriptionId);
        if (currentSubUsers >= sub.getGymPackage().getMaxSubUsers()) {
            throw new BusinessException(
                    "Maximum sub-users (" + sub.getGymPackage().getMaxSubUsers() + ") already reached");
        }

        Customer subCustomer = customerService.findOrThrow(request.getSubCustomerId());
        if (!subCustomer.getGym().getId().equals(gymId)) {
            throw new BusinessException("Sub-user must be a customer of the same gym");
        }

        // Main customer is the subscription owner
        Customer mainCustomer = sub.getCustomer();

        if (mainCustomer.getId().equals(subCustomer.getId())) {
            throw new BusinessException("Main customer cannot be added as their own sub-user");
        }

        if (membershipRepository.existsBySubscriptionIdAndSubCustomerId(subscriptionId, subCustomer.getId())) {
            throw new DuplicateResourceException("This customer is already a sub-user of this subscription");
        }

        FamilyMembership membership = FamilyMembership.builder()
                .mainCustomer(mainCustomer)
                .subCustomer(subCustomer)
                .subscription(sub)
                .relationType(request.getRelationType())
                .createdBy(emp)
                .build();

        return toResponse(membershipRepository.save(membership));
    }

    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> getSubUsers(Long gymId, Long subscriptionId, User currentUser) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        return membershipRepository.findBySubscriptionId(subscriptionId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void removeSubUser(Long gymId, Long subscriptionId, Long subCustomerId, User currentUser) {
        gymAccessService.resolveActingEmployee(currentUser, gymId, EmployeePermission.MANAGE_CUSTOMERS);
        findActiveSubscription(subscriptionId, gymId);

        if (!membershipRepository.existsBySubscriptionIdAndSubCustomerId(subscriptionId, subCustomerId)) {
            throw new ResourceNotFoundException("Sub-user not found in this subscription");
        }
        membershipRepository.deleteBySubscriptionIdAndSubCustomerId(subscriptionId, subCustomerId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Subscription findActiveSubscription(Long subscriptionId, Long gymId) {
        Subscription sub = subscriptionRepository.findByIdAndGymId(subscriptionId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", subscriptionId));
        if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessException("Sub-users can only be managed on ACTIVE subscriptions");
        }
        return sub;
    }

    private FamilyMemberResponse toResponse(FamilyMembership m) {
        return FamilyMemberResponse.builder()
                .id(m.getId())
                .mainCustomerId(m.getMainCustomer().getId())
                .subCustomerId(m.getSubCustomer().getId())
                .subCustomerFullName(m.getSubCustomer().getUser().getFullName())
                .subCustomerPhone(m.getSubCustomer().getUser().getPhone())
                .subscriptionId(m.getSubscription().getId())
                .relationType(m.getRelationType())
                .createdByEmployeeName(m.getCreatedBy() != null ? m.getCreatedBy().getUser().getFullName() : null)
                .createdAt(m.getCreatedAt())
                .build();
    }
}
