package com.gymhub.service;

import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.provider.Provider;
import com.gymhub.domain.provider.ProviderStatus;
import com.gymhub.domain.provider.ProviderType;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.provider.UpdateProviderRequest;
import com.gymhub.dto.response.provider.ProviderResponse;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.exception.UnauthorizedException;
import com.gymhub.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final ProviderRepository providerRepository;

    // ── Public queries ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProviderResponse> getMyProviders(User currentUser) {
        return providerRepository.findByOwnerUserId(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProviderResponse getProvider(Long providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider", providerId));
        return toResponse(provider);
    }

    @Transactional(readOnly = true)
    public Page<ProviderResponse> listProviders(ProviderType type, Pageable pageable) {
        Page<Provider> page = (type != null)
                ? providerRepository.findByProviderTypeAndActiveTrue(type, pageable)
                : providerRepository.findByActiveTrue(pageable);
        return page.map(this::toResponse);
    }

    // ── Updates ───────────────────────────────────────────────────────────────

    @Transactional
    public ProviderResponse updateProvider(Long providerId, UpdateProviderRequest request, User currentUser) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider", providerId));
        assertOwner(provider, currentUser);

        if (request.getDisplayName() != null) provider.setDisplayName(request.getDisplayName());
        if (request.getPhone() != null)       provider.setPhone(request.getPhone());
        if (request.getEmail() != null)       provider.setEmail(request.getEmail());
        if (request.getLogoUrl() != null)     provider.setLogoUrl(request.getLogoUrl());
        if (request.getCoverImageUrl() != null) provider.setCoverImageUrl(request.getCoverImageUrl());

        return toResponse(providerRepository.save(provider));
    }

    // ── Bridge: Gym → Provider ────────────────────────────────────────────────

    /**
     * Lazily creates (or returns existing) a Provider record bridging to the given Gym.
     * Called when Phase 2 modules need a Provider context for a gym that was created
     * before Phase 2, without requiring any migration.
     */
    @Transactional
    public Provider ensureProviderForGym(Gym gym) {
        return providerRepository.findByLinkedGymId(gym.getId())
                .orElseGet(() -> providerRepository.save(Provider.builder()
                        .providerType(ProviderType.GYM)
                        .ownerUser(gym.getOwner())
                        .linkedGym(gym)
                        .displayName(gym.getName())
                        .phone(gym.getPhone())
                        .email(gym.getEmail())
                        .logoUrl(gym.getLogoUrl())
                        .status(ProviderStatus.ACTIVE)
                        .active(true)
                        .build()));
    }

    /**
     * Creates a standalone SPECIALIST provider for a user who is not a gym owner.
     */
    @Transactional
    public Provider createSpecialistProvider(User ownerUser, String displayName) {
        return providerRepository.save(Provider.builder()
                .providerType(ProviderType.SPECIALIST)
                .ownerUser(ownerUser)
                .displayName(displayName)
                .status(ProviderStatus.ACTIVE)
                .active(true)
                .build());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Provider requireProvider(Long providerId) {
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider", providerId));
    }

    private void assertOwner(Provider provider, User user) {
        if (!provider.getOwnerUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own this provider profile");
        }
    }

    public ProviderResponse toResponse(Provider p) {
        return ProviderResponse.builder()
                .id(p.getId())
                .providerType(p.getProviderType())
                .ownerUserId(p.getOwnerUser().getId())
                .ownerName(p.getOwnerUser().getFullName())
                .linkedGymId(p.getLinkedGym() != null ? p.getLinkedGym().getId() : null)
                .displayName(p.getDisplayName())
                .phone(p.getPhone())
                .email(p.getEmail())
                .logoUrl(p.getLogoUrl())
                .coverImageUrl(p.getCoverImageUrl())
                .status(p.getStatus())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
