package com.gymhub.service;

import com.gymhub.domain.subscription.SubscriptionStatus;
import com.gymhub.domain.user.User;
import com.gymhub.dto.response.GymReportResponse;
import com.gymhub.repository.AttendanceRepository;
import com.gymhub.repository.CustomerRepository;
import com.gymhub.repository.PaymentRepository;
import com.gymhub.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * GD-15 Reports & Analytics — read-only operational summaries for a gym dashboard.
 * Aggregates existing Phase 1 data (payments, subscriptions, customers, attendance).
 */
@Service
@RequiredArgsConstructor
public class GymReportService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CustomerRepository customerRepository;
    private final AttendanceRepository attendanceRepository;
    private final GymAccessService gymAccessService;

    @Transactional(readOnly = true)
    public GymReportResponse getSummary(Long gymId, LocalDate fromDate, LocalDate toDate, User currentUser) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);

        LocalDate from = fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1);
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();

        return GymReportResponse.builder()
                .gymId(gymId)
                .fromDate(from)
                .toDate(to)
                .totalRevenueInRange(paymentRepository.sumByGymBetween(gymId, start, endExclusive))
                .cashRevenueInRange(paymentRepository.sumByGymAndMethodBetween(
                        gymId, com.gymhub.domain.subscription.PaymentMethod.CASH, start, endExclusive))
                .newSubscriptionsInRange(subscriptionRepository.countByGymIdAndSaleDateBetween(gymId, from, to))
                .activeSubscriptions(subscriptionRepository.countByGymIdAndStatus(gymId, SubscriptionStatus.ACTIVE))
                .pendingActivationSubscriptions(subscriptionRepository.countByGymIdAndStatus(
                        gymId, SubscriptionStatus.PENDING_ACTIVATION))
                .expiredSubscriptions(subscriptionRepository.countByGymIdAndStatus(gymId, SubscriptionStatus.EXPIRED))
                .totalCustomers(customerRepository.countByGymId(gymId))
                .activeCustomers(customerRepository.countByGymIdAndActiveTrue(gymId))
                .attendanceInRange(attendanceRepository.countByGymIdAndVisitedAtBetween(gymId, start, endExclusive))
                .build();
    }
}
