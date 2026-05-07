package com.gymhub.service;

import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.domain.freeze.FreezeStatus;
import com.gymhub.domain.freeze.SubscriptionFreeze;
import com.gymhub.domain.subscription.Subscription;
import com.gymhub.domain.subscription.SubscriptionStatus;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.FreezeRequest;
import com.gymhub.dto.response.FreezeResponse;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.repository.SubscriptionFreezeRepository;
import com.gymhub.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class FreezeService {

    private final SubscriptionFreezeRepository freezeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final GymAccessService gymAccessService;

    // ── Apply freeze ──────────────────────────────────────────────────────────

    @Transactional
    public FreezeResponse applyFreeze(Long gymId, Long subscriptionId,
                                      FreezeRequest request, User currentUser) {
        Employee emp = gymAccessService.resolveActingEmployee(
                currentUser, gymId, EmployeePermission.FREEZE_SUBSCRIPTION);

        Subscription sub = findActiveSubscription(subscriptionId, gymId);

        validateDates(request.getStartDate(), request.getEndDate());

        int requestedDays = (int) request.getStartDate().until(request.getEndDate(), ChronoUnit.DAYS);

        if (requestedDays <= 0) {
            throw new BusinessException("Freeze end date must be after start date");
        }
        if (requestedDays > sub.getRemainingFreezeDays()) {
            throw new BusinessException(
                    "Freeze of " + requestedDays + " days exceeds remaining freeze balance of " +
                    sub.getRemainingFreezeDays() + " days");
        }

        // Check for overlapping active freeze
        boolean hasOverlap = freezeRepository
                .existsBySubscriptionIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        subscriptionId, FreezeStatus.ACTIVE,
                        request.getEndDate(), request.getStartDate());
        if (hasOverlap) {
            throw new BusinessException("An active freeze already overlaps with the requested period");
        }

        // Extend subscription end date
        sub.setEndDate(sub.getEndDate().plusDays(requestedDays));
        sub.setUsedFreezeDays(sub.getUsedFreezeDays() + requestedDays);
        subscriptionRepository.save(sub);

        SubscriptionFreeze freeze = SubscriptionFreeze.builder()
                .subscription(sub)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .registeredBy(emp)
                .status(FreezeStatus.ACTIVE)
                .build();

        return toResponse(freezeRepository.save(freeze));
    }

    // ── Cancel freeze ─────────────────────────────────────────────────────────

    @Transactional
    public FreezeResponse cancelFreeze(Long gymId, Long subscriptionId,
                                       Long freezeId, User currentUser) {
        Employee emp = gymAccessService.resolveActingEmployee(
                currentUser, gymId, EmployeePermission.FREEZE_SUBSCRIPTION);

        Subscription sub = findActiveSubscription(subscriptionId, gymId);

        SubscriptionFreeze freeze = freezeRepository.findByIdAndSubscriptionId(freezeId, subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Freeze", freezeId));

        if (freeze.getStatus() != FreezeStatus.ACTIVE) {
            throw new BusinessException("Only ACTIVE freezes can be cancelled");
        }

        LocalDate today = LocalDate.now();
        LocalDate effectiveEnd = today.isBefore(freeze.getEndDate()) ? today : freeze.getEndDate();
        int daysActuallyUsed = (int) freeze.getStartDate().until(effectiveEnd, ChronoUnit.DAYS);
        if (daysActuallyUsed < 0) daysActuallyUsed = 0;

        int daysRefunded = freeze.plannedDays() - daysActuallyUsed;

        // Credit back unused freeze days and un-extend subscription end date
        sub.setUsedFreezeDays(sub.getUsedFreezeDays() - daysRefunded);
        sub.setEndDate(sub.getEndDate().minusDays(daysRefunded));
        subscriptionRepository.save(sub);

        freeze.setStatus(FreezeStatus.CANCELLED);
        freeze.setActualEndDate(effectiveEnd);
        freeze.setDaysConsumed(daysActuallyUsed);
        freeze.setCancelledBy(emp);
        freeze.setCancelledAt(LocalDateTime.now());

        return toResponse(freezeRepository.save(freeze));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<FreezeResponse> getFreezeHistory(Long gymId, Long subscriptionId,
                                                  User currentUser, Pageable pageable) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        return freezeRepository.findBySubscriptionId(subscriptionId, pageable).map(this::toResponse);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Subscription findActiveSubscription(Long subscriptionId, Long gymId) {
        Subscription sub = subscriptionRepository.findByIdAndGymId(subscriptionId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", subscriptionId));
        if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessException("Only ACTIVE subscriptions can be frozen");
        }
        return sub;
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.isBefore(LocalDate.now())) {
            throw new BusinessException("Freeze start date cannot be in the past");
        }
        if (!endDate.isAfter(startDate)) {
            throw new BusinessException("Freeze end date must be after start date");
        }
    }

    private FreezeResponse toResponse(SubscriptionFreeze f) {
        return FreezeResponse.builder()
                .id(f.getId())
                .subscriptionId(f.getSubscription().getId())
                .startDate(f.getStartDate())
                .endDate(f.getEndDate())
                .actualEndDate(f.getActualEndDate())
                .daysConsumed(f.getDaysConsumed())
                .reason(f.getReason())
                .status(f.getStatus())
                .registeredByEmployeeName(f.getRegisteredBy() != null
                        ? f.getRegisteredBy().getUser().getFullName() : null)
                .cancelledByEmployeeName(f.getCancelledBy() != null
                        ? f.getCancelledBy().getUser().getFullName() : null)
                .createdAt(f.getCreatedAt())
                .cancelledAt(f.getCancelledAt())
                .build();
    }
}
