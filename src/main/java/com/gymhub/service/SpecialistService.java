package com.gymhub.service;

import com.gymhub.domain.provider.Provider;
import com.gymhub.domain.specialist.*;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.specialist.CreateSpecialistProfileRequest;
import com.gymhub.dto.request.specialist.UpdateEnabledToolsRequest;
import com.gymhub.dto.request.specialist.UpdateServiceAxesRequest;
import com.gymhub.dto.response.specialist.EnabledToolResponse;
import com.gymhub.dto.response.specialist.ServiceAxisSelectionResponse;
import com.gymhub.dto.response.specialist.SpecialistProfileResponse;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.exception.UnauthorizedException;
import com.gymhub.repository.SpecialistEnabledToolRepository;
import com.gymhub.repository.SpecialistProfileRepository;
import com.gymhub.repository.SpecialistServiceAxisSelectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpecialistService {

    private final SpecialistProfileRepository profileRepository;
    private final SpecialistServiceAxisSelectionRepository axisRepo;
    private final SpecialistEnabledToolRepository toolRepo;
    private final ProviderService providerService;

    // ── Profile management ────────────────────────────────────────────────────

    @Transactional
    public SpecialistProfileResponse createProfile(CreateSpecialistProfileRequest request, User currentUser) {
        if (profileRepository.existsByUserId(currentUser.getId())) {
            throw new BusinessException("Specialist profile already exists for this account");
        }

        String displayName = (request.getDisplayName() != null && !request.getDisplayName().isBlank())
                ? request.getDisplayName()
                : currentUser.getFullName();

        Provider provider = providerService.createSpecialistProvider(currentUser, displayName);

        SpecialistProfile profile = SpecialistProfile.builder()
                .provider(provider)
                .user(currentUser)
                .mainSpecialization(request.getMainSpecialization())
                .additionalSpecializations(
                        request.getAdditionalSpecializations() != null
                                ? request.getAdditionalSpecializations()
                                : new ArrayList<>())
                .bio(request.getBio())
                .experienceYears(request.getExperienceYears())
                .profileImageUrl(request.getProfileImageUrl())
                .coverImageUrl(request.getCoverImageUrl())
                .worksIndependently(request.isWorksIndependently())
                .status(SpecialistStatus.ACTIVE)
                .active(true)
                .build();

        return toResponse(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public SpecialistProfileResponse getMyProfile(User currentUser) {
        return toResponse(resolveProfileForUser(currentUser));
    }

    @Transactional(readOnly = true)
    public SpecialistProfileResponse getPublicProfile(Long specialistId) {
        SpecialistProfile profile = profileRepository.findById(specialistId)
                .orElseThrow(() -> new ResourceNotFoundException("Specialist", specialistId));
        return toResponse(profile);
    }

    @Transactional
    public SpecialistProfileResponse updateMyProfile(CreateSpecialistProfileRequest request, User currentUser) {
        SpecialistProfile profile = resolveProfileForUser(currentUser);

        if (request.getMainSpecialization() != null)
            profile.setMainSpecialization(request.getMainSpecialization());
        if (request.getAdditionalSpecializations() != null)
            profile.setAdditionalSpecializations(request.getAdditionalSpecializations());
        if (request.getBio() != null)
            profile.setBio(request.getBio());
        if (request.getProfileImageUrl() != null)
            profile.setProfileImageUrl(request.getProfileImageUrl());
        if (request.getCoverImageUrl() != null)
            profile.setCoverImageUrl(request.getCoverImageUrl());
        profile.setExperienceYears(request.getExperienceYears());
        profile.setWorksIndependently(request.isWorksIndependently());

        return toResponse(profileRepository.save(profile));
    }

    // ── Service axes ──────────────────────────────────────────────────────────

    @Transactional
    public List<ServiceAxisSelectionResponse> updateServiceAxes(UpdateServiceAxesRequest request, User currentUser) {
        SpecialistProfile profile = resolveProfileForUser(currentUser);

        // Clear existing axes and tools, then rebuild
        axisRepo.deleteBySpecialistProfileId(profile.getId());
        toolRepo.deleteBySpecialistProfileId(profile.getId());

        Set<ToolCode> allEnabledTools = request.getSelectedAxes().stream()
                .flatMap(axis -> ToolCode.forAxis(axis).stream())
                .collect(Collectors.toSet());

        List<SpecialistServiceAxisSelection> saved = new ArrayList<>();
        for (ServiceAxis axis : request.getSelectedAxes()) {
            SpecialistServiceAxisSelection selection = SpecialistServiceAxisSelection.builder()
                    .specialistProfile(profile)
                    .serviceAxis(axis)
                    .isMainAxis(axis == request.getMainAxis())
                    .active(true)
                    .selectedAt(LocalDateTime.now())
                    .build();
            saved.add(axisRepo.save(selection));

            for (ToolCode tool : ToolCode.forAxis(axis)) {
                if (toolRepo.findBySpecialistProfileIdAndToolCode(profile.getId(), tool).isEmpty()) {
                    toolRepo.save(SpecialistEnabledTool.builder()
                            .specialistProfile(profile)
                            .toolCode(tool)
                            .serviceAxis(axis)
                            .enabled(true)
                            .autoEnabled(true)
                            .disabledBySpecialist(false)
                            .warningRequired(false)
                            .build());
                }
            }
        }

        return saved.stream().map(this::toAxisResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceAxisSelectionResponse> getMyServiceAxes(User currentUser) {
        SpecialistProfile profile = resolveProfileForUser(currentUser);
        return axisRepo.findBySpecialistProfileId(profile.getId())
                .stream().map(this::toAxisResponse).collect(Collectors.toList());
    }

    // ── Enabled tools ─────────────────────────────────────────────────────────

    @Transactional
    public List<EnabledToolResponse> updateEnabledTools(UpdateEnabledToolsRequest request, User currentUser) {
        SpecialistProfile profile = resolveProfileForUser(currentUser);

        // Collect which tool codes belong to active axes (needed for warningRequired)
        Set<ToolCode> axisCodes = axisRepo.findBySpecialistProfileId(profile.getId())
                .stream()
                .filter(SpecialistServiceAxisSelection::isActive)
                .flatMap(a -> ToolCode.forAxis(a.getServiceAxis()).stream())
                .collect(Collectors.toSet());

        for (UpdateEnabledToolsRequest.ToolToggle toggle : request.getTools()) {
            SpecialistEnabledTool tool = toolRepo
                    .findBySpecialistProfileIdAndToolCode(profile.getId(), toggle.getToolCode())
                    .orElseGet(() -> SpecialistEnabledTool.builder()
                            .specialistProfile(profile)
                            .toolCode(toggle.getToolCode())
                            .autoEnabled(false)
                            .build());

            tool.setEnabled(toggle.isEnabled());
            tool.setDisabledBySpecialist(!toggle.isEnabled());
            tool.setDisableReason(toggle.isEnabled() ? null : toggle.getDisableReason());
            // Warn if the specialist is disabling a tool that belongs to their active axes
            tool.setWarningRequired(!toggle.isEnabled() && axisCodes.contains(toggle.getToolCode()));
            toolRepo.save(tool);
        }

        return toolRepo.findBySpecialistProfileId(profile.getId())
                .stream().map(this::toToolResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EnabledToolResponse> getMyEnabledTools(User currentUser) {
        SpecialistProfile profile = resolveProfileForUser(currentUser);
        return toolRepo.findBySpecialistProfileId(profile.getId())
                .stream().map(this::toToolResponse).collect(Collectors.toList());
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    public SpecialistProfile resolveProfileForUser(User user) {
        return profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Specialist profile not found for this account"));
    }

    public SpecialistProfile resolveProfileById(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Specialist", profileId));
    }

    private SpecialistProfileResponse toResponse(SpecialistProfile p) {
        return SpecialistProfileResponse.builder()
                .id(p.getId())
                .providerId(p.getProvider().getId())
                .userId(p.getUser().getId())
                .displayName(p.getProvider().getDisplayName())
                .mainSpecialization(p.getMainSpecialization())
                .additionalSpecializations(p.getAdditionalSpecializations())
                .bio(p.getBio())
                .experienceYears(p.getExperienceYears())
                .profileImageUrl(p.getProfileImageUrl())
                .coverImageUrl(p.getCoverImageUrl())
                .worksIndependently(p.isWorksIndependently())
                .status(p.getStatus())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private ServiceAxisSelectionResponse toAxisResponse(SpecialistServiceAxisSelection s) {
        return ServiceAxisSelectionResponse.builder()
                .id(s.getId())
                .serviceAxis(s.getServiceAxis())
                .isMainAxis(s.isMainAxis())
                .active(s.isActive())
                .selectedAt(s.getSelectedAt())
                .build();
    }

    private EnabledToolResponse toToolResponse(SpecialistEnabledTool t) {
        return EnabledToolResponse.builder()
                .id(t.getId())
                .toolCode(t.getToolCode())
                .serviceAxis(t.getServiceAxis())
                .enabled(t.isEnabled())
                .autoEnabled(t.isAutoEnabled())
                .disabledBySpecialist(t.isDisabledBySpecialist())
                .disableReason(t.getDisableReason())
                .warningRequired(t.isWarningRequired())
                .build();
    }
}
