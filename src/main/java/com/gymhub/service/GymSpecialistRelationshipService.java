package com.gymhub.service;

import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.provider.Provider;
import com.gymhub.domain.relationship.*;
import com.gymhub.domain.specialist.SpecialistProfile;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.relationship.CreateGymRelationshipRequest;
import com.gymhub.dto.request.relationship.CreateSpecialistRelationshipRequest;
import com.gymhub.dto.request.relationship.UpdateRelationshipPolicyRequest;
import com.gymhub.dto.response.relationship.GymSpecialistRelationshipResponse;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.exception.UnauthorizedException;
import com.gymhub.repository.GymSpecialistRelationshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GymSpecialistRelationshipService {

    private final GymSpecialistRelationshipRepository relationshipRepo;
    private final GymAccessService gymAccessService;
    private final ProviderService providerService;
    private final SpecialistService specialistService;

    // ── Create requests ───────────────────────────────────────────────────────

    @Transactional
    public GymSpecialistRelationshipResponse requestFromGym(
            Long gymId, CreateGymRelationshipRequest request, User currentUser) {

        gymAccessService.resolveActingEmployee(
                currentUser, gymId, EmployeePermission.MANAGE_SPECIALIST_RELATIONSHIPS);

        Gym gym = gymAccessService.requireGym(gymId);
        Provider gymProvider = providerService.ensureProviderForGym(gym);

        SpecialistProfile specialistProfile = specialistService.resolveProfileById(request.getSpecialistProfileId());

        assertNoActiveRelationship(gymId, specialistProfile.getId());

        GymSpecialistRelationship rel = GymSpecialistRelationship.builder()
                .gym(gym)
                .gymProvider(gymProvider)
                .specialistProvider(specialistProfile.getProvider())
                .specialistProfile(specialistProfile)
                .requestedByProvider(gymProvider)
                .requestInitiatorType(RequestInitiatorType.GYM)
                .relationshipType(request.getRelationshipType())
                .status(RelationshipStatus.REQUESTED)
                .startDate(request.getProposedStartDate())
                .paymentOwner(resolveOrDefault(request.getPaymentOwner(), CommercialOwner.GYM))
                .activationOwner(resolveOrDefault(request.getActivationOwner(), CommercialOwner.GYM))
                .serviceOwner(resolveOrDefault(request.getServiceOwner(), CommercialOwner.GYM))
                .packageOwner(resolveOrDefault(request.getPackageOwner(), CommercialOwner.GYM))
                .canSpecialistSellToGymClients(request.isCanSpecialistSellToGymClients())
                .canSpecialistSellToNonGymClients(request.isCanSpecialistSellToNonGymClients())
                .existingSubscriptionsRemainOriginalOwner(true)
                .gymPatternPolicy(resolveOrDefault(request.getGymPatternPolicy(), GymPatternPolicy.NOT_APPLICABLE))
                .mandatoryPatternsApply(request.isMandatoryPatternsApply())
                .recommendedPatternsVisible(request.isRecommendedPatternsVisible())
                .notes(request.getNotes())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        return toResponse(relationshipRepo.save(rel));
    }

    @Transactional
    public GymSpecialistRelationshipResponse requestFromSpecialist(
            CreateSpecialistRelationshipRequest request, User currentUser) {

        SpecialistProfile specialistProfile = specialistService.resolveProfileForUser(currentUser);
        Gym gym = gymAccessService.requireGym(request.getGymId());
        Provider gymProvider = providerService.ensureProviderForGym(gym);

        assertNoActiveRelationship(request.getGymId(), specialistProfile.getId());

        GymSpecialistRelationship rel = GymSpecialistRelationship.builder()
                .gym(gym)
                .gymProvider(gymProvider)
                .specialistProvider(specialistProfile.getProvider())
                .specialistProfile(specialistProfile)
                .requestedByProvider(specialistProfile.getProvider())
                .requestInitiatorType(RequestInitiatorType.SPECIALIST)
                .relationshipType(request.getRelationshipType())
                .status(RelationshipStatus.REQUESTED)
                .startDate(request.getProposedStartDate())
                .existingSubscriptionsRemainOriginalOwner(true)
                .notes(request.getNotes())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        return toResponse(relationshipRepo.save(rel));
    }

    // ── Status transitions ────────────────────────────────────────────────────

    @Transactional
    public GymSpecialistRelationshipResponse accept(Long relationshipId, User currentUser) {
        GymSpecialistRelationship rel = requireRelationship(relationshipId);
        assertResponderCanAct(rel, currentUser);
        assertStatus(rel, RelationshipStatus.REQUESTED);

        rel.setStatus(RelationshipStatus.ACTIVE);
        rel.setStartDate(rel.getStartDate() != null ? rel.getStartDate() : LocalDate.now());
        rel.setUpdatedBy(currentUser);
        return toResponse(relationshipRepo.save(rel));
    }

    @Transactional
    public GymSpecialistRelationshipResponse reject(Long relationshipId, User currentUser) {
        GymSpecialistRelationship rel = requireRelationship(relationshipId);
        assertResponderCanAct(rel, currentUser);
        assertStatus(rel, RelationshipStatus.REQUESTED);

        rel.setStatus(RelationshipStatus.REJECTED);
        rel.setUpdatedBy(currentUser);
        return toResponse(relationshipRepo.save(rel));
    }

    @Transactional
    public GymSpecialistRelationshipResponse suspend(Long relationshipId, User currentUser) {
        GymSpecialistRelationship rel = requireRelationship(relationshipId);
        assertParticipant(rel, currentUser);
        assertStatus(rel, RelationshipStatus.ACTIVE);

        rel.setStatus(RelationshipStatus.SUSPENDED);
        rel.setUpdatedBy(currentUser);
        return toResponse(relationshipRepo.save(rel));
    }

    @Transactional
    public GymSpecialistRelationshipResponse terminate(Long relationshipId, User currentUser) {
        GymSpecialistRelationship rel = requireRelationship(relationshipId);
        assertParticipant(rel, currentUser);

        rel.setStatus(RelationshipStatus.TERMINATED);
        rel.setEndDate(LocalDate.now());
        rel.setUpdatedBy(currentUser);
        return toResponse(relationshipRepo.save(rel));
    }

    @Transactional
    public GymSpecialistRelationshipResponse updatePolicy(
            Long relationshipId, UpdateRelationshipPolicyRequest request, User currentUser) {

        GymSpecialistRelationship rel = requireRelationship(relationshipId);
        assertParticipant(rel, currentUser);

        if (request.getPaymentOwner() != null)    rel.setPaymentOwner(request.getPaymentOwner());
        if (request.getActivationOwner() != null)  rel.setActivationOwner(request.getActivationOwner());
        if (request.getServiceOwner() != null)     rel.setServiceOwner(request.getServiceOwner());
        if (request.getPackageOwner() != null)     rel.setPackageOwner(request.getPackageOwner());
        if (request.getCanSpecialistSellToGymClients() != null)
            rel.setCanSpecialistSellToGymClients(request.getCanSpecialistSellToGymClients());
        if (request.getCanSpecialistSellToNonGymClients() != null)
            rel.setCanSpecialistSellToNonGymClients(request.getCanSpecialistSellToNonGymClients());
        if (request.getGymPatternPolicy() != null) rel.setGymPatternPolicy(request.getGymPatternPolicy());
        if (request.getMandatoryPatternsApply() != null)
            rel.setMandatoryPatternsApply(request.getMandatoryPatternsApply());
        if (request.getRecommendedPatternsVisible() != null)
            rel.setRecommendedPatternsVisible(request.getRecommendedPatternsVisible());
        if (request.getNotes() != null) rel.setNotes(request.getNotes());
        rel.setUpdatedBy(currentUser);

        return toResponse(relationshipRepo.save(rel));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<GymSpecialistRelationshipResponse> getGymRelationships(Long gymId, User currentUser, Pageable pageable) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        return relationshipRepo.findByGymId(gymId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<GymSpecialistRelationshipResponse> getSpecialistRelationships(User currentUser, Pageable pageable) {
        SpecialistProfile profile = specialistService.resolveProfileForUser(currentUser);
        return relationshipRepo.findBySpecialistProfileId(profile.getId(), pageable).map(this::toResponse);
    }

    // ── Guards ────────────────────────────────────────────────────────────────

    private void assertNoActiveRelationship(Long gymId, Long specialistProfileId) {
        boolean exists = relationshipRepo.existsByGymIdAndSpecialistProfileIdAndStatusIn(
                gymId, specialistProfileId,
                List.of(RelationshipStatus.REQUESTED, RelationshipStatus.ACTIVE));
        if (exists) {
            throw new BusinessException("A relationship request or active relationship already exists between this gym and specialist");
        }
    }

    private void assertResponderCanAct(GymSpecialistRelationship rel, User user) {
        boolean isGymOwner = rel.getGym().getOwner().getId().equals(user.getId());
        boolean isSpecialist = rel.getSpecialistProfile().getUser().getId().equals(user.getId());

        if (rel.getRequestInitiatorType() == RequestInitiatorType.GYM && !isSpecialist) {
            throw new UnauthorizedException("Only the specialist can respond to a gym-initiated request");
        }
        if (rel.getRequestInitiatorType() == RequestInitiatorType.SPECIALIST && !isGymOwner) {
            throw new UnauthorizedException("Only the gym owner can respond to a specialist-initiated request");
        }
    }

    private void assertParticipant(GymSpecialistRelationship rel, User user) {
        boolean isGymOwner = rel.getGym().getOwner().getId().equals(user.getId());
        boolean isSpecialist = rel.getSpecialistProfile().getUser().getId().equals(user.getId());
        if (!isGymOwner && !isSpecialist) {
            throw new UnauthorizedException("You are not a participant in this relationship");
        }
    }

    private void assertStatus(GymSpecialistRelationship rel, RelationshipStatus expected) {
        if (rel.getStatus() != expected) {
            throw new BusinessException(
                    "Invalid status transition from " + rel.getStatus() + " — expected " + expected);
        }
    }

    private GymSpecialistRelationship requireRelationship(Long id) {
        return relationshipRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Relationship", id));
    }

    private <T> T resolveOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private GymSpecialistRelationshipResponse toResponse(GymSpecialistRelationship r) {
        return GymSpecialistRelationshipResponse.builder()
                .id(r.getId())
                .gymId(r.getGym().getId())
                .gymName(r.getGym().getName())
                .specialistProfileId(r.getSpecialistProfile().getId())
                .specialistDisplayName(r.getSpecialistProfile().getProvider().getDisplayName())
                .specialistMainSpecialization(r.getSpecialistProfile().getMainSpecialization().name())
                .requestInitiatorType(r.getRequestInitiatorType())
                .relationshipType(r.getRelationshipType())
                .status(r.getStatus())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .paymentOwner(r.getPaymentOwner())
                .activationOwner(r.getActivationOwner())
                .serviceOwner(r.getServiceOwner())
                .packageOwner(r.getPackageOwner())
                .canSpecialistSellToGymClients(r.isCanSpecialistSellToGymClients())
                .canSpecialistSellToNonGymClients(r.isCanSpecialistSellToNonGymClients())
                .existingSubscriptionsRemainOriginalOwner(r.isExistingSubscriptionsRemainOriginalOwner())
                .allowGymTrainingPatterns(r.isAllowGymTrainingPatterns())
                .gymPatternPolicy(r.getGymPatternPolicy())
                .mandatoryPatternsApply(r.isMandatoryPatternsApply())
                .recommendedPatternsVisible(r.isRecommendedPatternsVisible())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
