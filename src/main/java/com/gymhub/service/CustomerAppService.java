package com.gymhub.service;

import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.customer.GymLinkRequest;
import com.gymhub.domain.customer.GymLinkRequestStatus;
import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.invitation.Invitation;
import com.gymhub.domain.subscription.Subscription;
import com.gymhub.domain.user.User;
import com.gymhub.dto.response.*;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.DuplicateResourceException;
import com.gymhub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Customer-facing self-service operations.
 *
 * Reuses existing repositories and domain objects — no business logic is duplicated.
 * All methods are scoped strictly to the authenticated user's own data.
 */
@Service
@RequiredArgsConstructor
public class CustomerAppService {

    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AttendanceRepository attendanceRepository;
    private final InvitationRepository invitationRepository;
    private final GymRepository gymRepository;
    private final GymLinkRequestRepository linkRequestRepository;

    // ── Profile ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(User currentUser) {
        return UserProfileResponse.builder()
                .id(currentUser.getId())
                .firstName(currentUser.getFirstName())
                .lastName(currentUser.getLastName())
                .email(currentUser.getEmail())
                .phone(currentUser.getPhone())
                .profileImageUrl(currentUser.getProfileImageUrl())
                .roles(currentUser.getRoles())
                .build();
    }

    // ── Gyms ──────────────────────────────────────────────────────────────────

    /**
     * Returns all gyms where the customer has an active customer record.
     */
    @Transactional(readOnly = true)
    public List<GymResponse> getMyGyms(User currentUser) {
        return customerRepository.findByUserId(currentUser.getId())
                .stream()
                .filter(Customer::isActive)
                .map(c -> gymToResponse(c.getGym()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GymResponse getGymDetails(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new com.gymhub.exception.ResourceNotFoundException("Gym", gymId));
        return gymToResponse(gym);
    }

    // ── Gym link request ──────────────────────────────────────────────────────

    @Transactional
    public GymLinkRequestResponse requestGymLink(Long gymId, String notes, User currentUser) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new com.gymhub.exception.ResourceNotFoundException("Gym", gymId));

        // Cannot request if already a customer
        if (customerRepository.existsByUserIdAndGymId(currentUser.getId(), gymId)) {
            throw new BusinessException("You are already a customer of this gym");
        }

        // Cannot have an active pending request
        if (linkRequestRepository.existsByUserIdAndGymId(currentUser.getId(), gymId)) {
            throw new DuplicateResourceException("A link request for this gym already exists");
        }

        GymLinkRequest request = GymLinkRequest.builder()
                .user(currentUser)
                .gym(gym)
                .notes(notes)
                .status(GymLinkRequestStatus.PENDING)
                .build();

        return toLinkResponse(linkRequestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<GymLinkRequestResponse> getMyLinkRequests(User currentUser) {
        return linkRequestRepository.findByUserId(currentUser.getId())
                .stream().map(this::toLinkResponse).collect(Collectors.toList());
    }

    // ── Subscriptions ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getMySubscriptions(User currentUser, Pageable pageable) {
        // Find all customer records for this user
        List<Long> customerIds = customerRepository.findByUserId(currentUser.getId())
                .stream().map(Customer::getId).collect(Collectors.toList());

        return subscriptionRepository.findByCustomerIdIn(customerIds, pageable)
                .map(this::subToResponse);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getMySubscription(Long subscriptionId, User currentUser) {
        List<Long> customerIds = customerRepository.findByUserId(currentUser.getId())
                .stream().map(Customer::getId).collect(Collectors.toList());

        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .filter(s -> customerIds.contains(s.getCustomer().getId()))
                .orElseThrow(() -> new com.gymhub.exception.ResourceNotFoundException("Subscription", subscriptionId));

        return subToResponse(sub);
    }

    // ── Attendance ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getMyAttendance(User currentUser, Pageable pageable) {
        List<Long> customerIds = customerRepository.findByUserId(currentUser.getId())
                .stream().map(Customer::getId).collect(Collectors.toList());

        return attendanceRepository.findByCustomerIdInOrderByVisitedAtDesc(customerIds, pageable)
                .map(a -> AttendanceResponse.builder()
                        .id(a.getId())
                        .gymId(a.getGym().getId())
                        .gymName(a.getGym().getName())
                        .type(a.getType())
                        .entranceMethod(a.getEntranceMethod())
                        .visitedAt(a.getVisitedAt())
                        .build());
    }

    // ── Invitations ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<InvitationResponse> getMyInvitations(User currentUser, Pageable pageable) {
        List<Long> customerIds = customerRepository.findByUserId(currentUser.getId())
                .stream().map(Customer::getId).collect(Collectors.toList());

        return invitationRepository.findByHostIdIn(customerIds, pageable)
                .map(i -> InvitationResponse.builder()
                        .id(i.getId())
                        .gymId(i.getGym().getId())
                        .gymName(i.getGym().getName())
                        .subscriptionId(i.getSubscription().getId())
                        .guestFullName(i.getGuestUser().getFullName())
                        .guestRepeatVisitAllowed(i.isGuestRepeatVisitAllowed())
                        .usedAt(i.getUsedAt())
                        .build());
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private GymResponse gymToResponse(Gym gym) {
        return GymResponse.builder()
                .id(gym.getId())
                .name(gym.getName())
                .description(gym.getDescription())
                .logoUrl(gym.getLogoUrl())
                .address(gym.getAddress())
                .city(gym.getCity())
                .country(gym.getCountry())
                .phone(gym.getPhone())
                .email(gym.getEmail())
                .website(gym.getWebsite())
                .status(gym.getStatus())
                .ownerId(gym.getOwner().getId())
                .ownerName(gym.getOwner().getFullName())
                .createdAt(gym.getCreatedAt())
                .build();
    }

    private SubscriptionResponse subToResponse(Subscription sub) {
        return SubscriptionResponse.builder()
                .id(sub.getId())
                .customerId(sub.getCustomer().getId())
                .customerName(sub.getCustomer().getUser().getFullName())
                .packageId(sub.getGymPackage().getId())
                .packageName(sub.getGymPackage().getName())
                .gymId(sub.getGym().getId())
                .totalPrice(sub.getTotalPrice())
                .paidAmount(sub.getPaidAmount())
                .remainingAmount(sub.getRemainingAmount())
                .currency(sub.getCurrency())
                .status(sub.getStatus())
                .saleDate(sub.getSaleDate())
                .activationDate(sub.getActivationDate())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .maxInvitations(sub.getGymPackage().getMaxInvitations())
                .usedInvitations(sub.getUsedInvitations())
                .remainingInvitations(sub.getRemainingInvitations())
                .usedFreezeDays(sub.getUsedFreezeDays())
                .remainingFreezeDays(sub.getRemainingFreezeDays())
                .activationType(sub.getActivationType())
                .soldByEmployeeName(sub.getSoldBy() != null ? sub.getSoldBy().getUser().getFullName() : null)
                .activatedByEmployeeName(sub.getActivatedBy() != null ? sub.getActivatedBy().getUser().getFullName() : null)
                .createdAt(sub.getCreatedAt())
                .build();
    }

    private GymLinkRequestResponse toLinkResponse(GymLinkRequest r) {
        return GymLinkRequestResponse.builder()
                .id(r.getId())
                .gymId(r.getGym().getId())
                .gymName(r.getGym().getName())
                .status(r.getStatus())
                .notes(r.getNotes())
                .requestedAt(r.getRequestedAt())
                .resolvedAt(r.getResolvedAt())
                .build();
    }
}
