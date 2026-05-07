package com.gymhub.service;

import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.customer.GymLinkRequest;
import com.gymhub.domain.customer.GymLinkRequestStatus;
import com.gymhub.domain.gym.*;
import com.gymhub.domain.user.User;
import com.gymhub.domain.user.UserRole;
import com.gymhub.dto.request.CreateGymRequest;
import com.gymhub.dto.request.GymSettingsRequest;
import com.gymhub.dto.response.GymLinkRequestResponse;
import com.gymhub.dto.response.GymResponse;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.exception.UnauthorizedException;
import com.gymhub.repository.CustomerRepository;
import com.gymhub.repository.GymLinkRequestRepository;
import com.gymhub.repository.GymRepository;
import com.gymhub.repository.GymSettingsRepository;
import com.gymhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GymManagementService {

    private final GymRepository gymRepository;
    private final GymSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final GymLinkRequestRepository linkRequestRepository;
    private final CustomerRepository customerRepository;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public GymResponse createGym(CreateGymRequest request, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + ownerEmail));

        // Ensure the user has SERVICE_PROVIDER role
        if (!owner.getRoles().contains(UserRole.SERVICE_PROVIDER)) {
            owner.addRole(UserRole.SERVICE_PROVIDER);
            userRepository.save(owner);
        }

        Gym gym = Gym.builder()
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .city(request.getCity())
                .country(request.getCountry())
                .phone(request.getPhone())
                .email(request.getEmail())
                .website(request.getWebsite())
                .owner(owner)
                .status(GymStatus.ACTIVE)
                .build();

        gym = gymRepository.save(gym);

        // Create default settings
        GymSettings settings = GymSettings.builder()
                .gym(gym)
                .activationPolicy(ActivationPolicy.IMMEDIATE)
                .allowPartialPayment(false)
                .build();
        settingsRepository.save(settings);

        return toResponse(gym);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public GymResponse getGym(Long gymId) {
        return toResponse(findGymOrThrow(gymId));
    }

    @Transactional(readOnly = true)
    public List<GymResponse> getMyGyms(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return gymRepository.findByOwnerId(owner.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public GymResponse updateGym(Long gymId, CreateGymRequest request, String ownerEmail) {
        Gym gym = findGymOrThrow(gymId);
        assertOwner(gym, ownerEmail);

        gym.setName(request.getName());
        gym.setDescription(request.getDescription());
        gym.setAddress(request.getAddress());
        gym.setCity(request.getCity());
        gym.setCountry(request.getCountry());
        gym.setPhone(request.getPhone());
        gym.setEmail(request.getEmail());
        gym.setWebsite(request.getWebsite());

        return toResponse(gymRepository.save(gym));
    }

    @Transactional
    public void updateSettings(Long gymId, GymSettingsRequest request, String ownerEmail) {
        Gym gym = findGymOrThrow(gymId);
        assertOwner(gym, ownerEmail);

        GymSettings settings = settingsRepository.findByGymId(gymId)
                .orElse(GymSettings.builder().gym(gym).build());

        settings.setAllowPartialPayment(request.isAllowPartialPayment());
        settings.setActivationPolicy(request.getActivationPolicy());
        settings.setAllowActivationWithRemainingBalance(request.isAllowActivationWithRemainingBalance());
        if (request.getEnabledEntranceMethods() != null) {
            settings.setEnabledEntranceMethods(request.getEnabledEntranceMethods());
        }
        settingsRepository.save(settings);
    }

    @Transactional
    public void updateStatus(Long gymId, GymStatus status, String ownerEmail) {
        Gym gym = findGymOrThrow(gymId);
        assertOwner(gym, ownerEmail);
        gym.setStatus(status);
        gymRepository.save(gym);
    }

    // ── Gym link requests (dashboard side) ───────────────────────────────────

    @Transactional(readOnly = true)
    public List<GymLinkRequestResponse> getPendingLinkRequests(Long gymId, String ownerEmail) {
        Gym gym = findGymOrThrow(gymId);
        assertOwner(gym, ownerEmail);
        return linkRequestRepository.findByGymIdAndStatus(gymId, GymLinkRequestStatus.PENDING)
                .stream().map(this::toLinkResponse).collect(Collectors.toList());
    }

    @Transactional
    public void approveLinkRequest(Long gymId, Long requestId, String ownerEmail) {
        Gym gym = findGymOrThrow(gymId);
        assertOwner(gym, ownerEmail);

        GymLinkRequest linkRequest = linkRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("GymLinkRequest", requestId));

        if (!linkRequest.getGym().getId().equals(gymId)) {
            throw new BusinessException("Link request does not belong to this gym");
        }
        if (linkRequest.getStatus() != GymLinkRequestStatus.PENDING) {
            throw new BusinessException("Link request is not in PENDING status");
        }

        User user = linkRequest.getUser();

        if (!customerRepository.existsByUserIdAndGymId(user.getId(), gymId)) {
            if (!user.getRoles().contains(UserRole.CUSTOMER)) {
                user.addRole(UserRole.CUSTOMER);
                userRepository.save(user);
            }
            Customer customer = Customer.builder()
                    .user(user)
                    .gym(gym)
                    .memberCode("GH" + gymId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .active(true)
                    .build();
            customerRepository.save(customer);
        }

        linkRequest.setStatus(GymLinkRequestStatus.APPROVED);
        linkRequest.setResolvedAt(LocalDateTime.now());
        linkRequestRepository.save(linkRequest);
    }

    @Transactional
    public void rejectLinkRequest(Long gymId, Long requestId, String ownerEmail) {
        Gym gym = findGymOrThrow(gymId);
        assertOwner(gym, ownerEmail);

        GymLinkRequest linkRequest = linkRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("GymLinkRequest", requestId));

        if (!linkRequest.getGym().getId().equals(gymId)) {
            throw new BusinessException("Link request does not belong to this gym");
        }
        if (linkRequest.getStatus() != GymLinkRequestStatus.PENDING) {
            throw new BusinessException("Link request is not in PENDING status");
        }

        linkRequest.setStatus(GymLinkRequestStatus.REJECTED);
        linkRequest.setResolvedAt(LocalDateTime.now());
        linkRequestRepository.save(linkRequest);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public Gym findGymOrThrow(Long gymId) {
        return gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Gym", gymId));
    }

    private void assertOwner(Gym gym, String email) {
        if (!gym.getOwner().getEmail().equals(email)) {
            throw new UnauthorizedException("You are not the owner of this gym");
        }
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

    private GymResponse toResponse(Gym gym) {
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
}
