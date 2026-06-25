package com.gymhub.service;

import com.gymhub.domain.cashday.CashDayClosing;
import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.subscription.PaymentMethod;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.CashDayClosingRequest;
import com.gymhub.dto.response.CashDayClosingResponse;
import com.gymhub.exception.BusinessException;
import com.gymhub.repository.CashDayClosingRepository;
import com.gymhub.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * GD-07 Cash Payments & Daily Closing.
 *
 * <p>Computes the expected CASH total recorded for a gym on a business date and reconciles it
 * against the physically counted drawer amount. Closing is a one-per-day financial record.</p>
 */
@Service
@RequiredArgsConstructor
public class CashDayClosingService {

    private final CashDayClosingRepository closingRepository;
    private final PaymentRepository paymentRepository;
    private final GymAccessService gymAccessService;

    // ── Preview (no write) ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CashDayClosingResponse preview(Long gymId, LocalDate date, User currentUser) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        LocalDate businessDate = date != null ? date : LocalDate.now();
        LocalDateTime start = businessDate.atStartOfDay();
        LocalDateTime end = businessDate.plusDays(1).atStartOfDay();

        BigDecimal expected = paymentRepository.sumByGymAndMethodBetween(
                gymId, PaymentMethod.CASH, start, end);
        long count = paymentRepository.countByGymAndMethodBetween(
                gymId, PaymentMethod.CASH, start, end);

        return CashDayClosingResponse.builder()
                .gymId(gymId)
                .businessDate(businessDate)
                .expectedCashTotal(expected)
                .countedCashTotal(null)
                .variance(null)
                .paymentCount((int) count)
                .build();
    }

    // ── Close ─────────────────────────────────────────────────────────────────

    @Transactional
    public CashDayClosingResponse closeDay(Long gymId, CashDayClosingRequest request, User currentUser) {
        Employee actingEmployee = gymAccessService.resolveActingEmployee(
                currentUser, gymId, EmployeePermission.CLOSE_CASH_DAY);
        Gym gym = gymAccessService.requireGym(gymId);

        LocalDate businessDate = request.getBusinessDate() != null
                ? request.getBusinessDate() : LocalDate.now();

        if (closingRepository.existsByGymIdAndBusinessDate(gymId, businessDate)) {
            throw new BusinessException("Cash day for " + businessDate + " is already closed");
        }

        LocalDateTime start = businessDate.atStartOfDay();
        LocalDateTime end = businessDate.plusDays(1).atStartOfDay();
        BigDecimal expected = paymentRepository.sumByGymAndMethodBetween(
                gymId, PaymentMethod.CASH, start, end);
        long count = paymentRepository.countByGymAndMethodBetween(
                gymId, PaymentMethod.CASH, start, end);

        BigDecimal counted = request.getCountedCashTotal();
        BigDecimal variance = counted.subtract(expected);

        CashDayClosing closing = CashDayClosing.builder()
                .gym(gym)
                .businessDate(businessDate)
                .expectedCashTotal(expected)
                .countedCashTotal(counted)
                .variance(variance)
                .paymentCount((int) count)
                .notes(request.getNotes())
                .closedBy(actingEmployee)
                .build();

        return toResponse(closingRepository.save(closing));
    }

    // ── History ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CashDayClosingResponse> getHistory(Long gymId, User currentUser, Pageable pageable) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        return closingRepository.findByGymIdOrderByBusinessDateDesc(gymId, pageable)
                .map(this::toResponse);
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private CashDayClosingResponse toResponse(CashDayClosing c) {
        return CashDayClosingResponse.builder()
                .id(c.getId())
                .gymId(c.getGym().getId())
                .businessDate(c.getBusinessDate())
                .expectedCashTotal(c.getExpectedCashTotal())
                .countedCashTotal(c.getCountedCashTotal())
                .variance(c.getVariance())
                .paymentCount(c.getPaymentCount())
                .notes(c.getNotes())
                .closedByEmployeeName(c.getClosedBy() != null
                        ? c.getClosedBy().getUser().getFullName() : null)
                .closedAt(c.getClosedAt())
                .build();
    }
}
