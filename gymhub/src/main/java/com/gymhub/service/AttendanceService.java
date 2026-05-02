package com.gymhub.service;

import com.gymhub.domain.attendance.Attendance;
import com.gymhub.domain.attendance.AttendanceType;
import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.domain.subscription.Subscription;
import com.gymhub.domain.subscription.SubscriptionStatus;
import com.gymhub.dto.request.RecordAttendanceRequest;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.exception.UnauthorizedException;
import com.gymhub.repository.AttendanceRepository;
import com.gymhub.repository.CustomerRepository;
import com.gymhub.repository.SubscriptionRepository;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EmployeeManagementService employeeService;

    @Transactional
    public AttendanceSummary recordAttendance(Long gymId, RecordAttendanceRequest request,
                                               Long employeeId) {
        Employee emp = employeeService.findOrThrow(employeeId);
        if (!emp.hasPermission(EmployeePermission.REGISTER_ATTENDANCE)) {
            throw new UnauthorizedException("Employee does not have permission to register attendance");
        }

        // Resolve customer
        Customer customer = resolveCustomer(request, gymId);

        // Resolve subscription (for MEMBER_VISIT)
        Subscription sub = null;
        if (request.getType() == AttendanceType.MEMBER_VISIT) {
            sub = resolveActiveSubscription(customer.getId(), gymId, request.getSubscriptionId());
        }

        Attendance attendance = Attendance.builder()
                .customer(customer)
                .gym(customer.getGym())
                .type(request.getType())
                .entranceMethod(request.getEntranceMethod())
                .subscription(sub)
                .recordedBy(emp)
                .build();

        attendanceRepository.save(attendance);

        return AttendanceSummary.builder()
                .customerId(customer.getId())
                .customerName(customer.getUser().getFullName())
                .memberCode(customer.getMemberCode())
                .profileImageUrl(customer.getUser().getProfileImageUrl())
                .subscriptionId(sub != null ? sub.getId() : null)
                .subscriptionStatus(sub != null ? sub.getStatus().name() : null)
                .visitedAt(attendance.getVisitedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<Attendance> getGymAttendance(Long gymId, Pageable pageable) {
        return attendanceRepository.findByGymIdOrderByVisitedAtDesc(gymId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Attendance> getCustomerAttendance(Long customerId, Pageable pageable) {
        return attendanceRepository.findByCustomerIdOrderByVisitedAtDesc(customerId, pageable);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Customer resolveCustomer(RecordAttendanceRequest request, Long gymId) {
        if (request.getMemberCode() != null) {
            return customerRepository.findByMemberCode(request.getMemberCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No customer found for member code: " + request.getMemberCode()));
        }
        if (request.getCustomerId() != null) {
            Customer c = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId()));
            if (!c.getGym().getId().equals(gymId)) {
                throw new BusinessException("Customer does not belong to this gym");
            }
            return c;
        }
        throw new BusinessException("Either memberCode or customerId must be provided");
    }

    private Subscription resolveActiveSubscription(Long customerId, Long gymId, Long subscriptionId) {
        if (subscriptionId != null) {
            Subscription sub = subscriptionRepository.findById(subscriptionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subscription", subscriptionId));
            if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
                throw new BusinessException("Subscription is not active");
            }
            return sub;
        }

        // Auto-resolve: pick the first active subscription for this customer/gym
        List<Subscription> active = subscriptionRepository
                .findByCustomerIdAndStatus(customerId, SubscriptionStatus.ACTIVE);
        return active.stream()
                .filter(s -> s.getGym().getId().equals(gymId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("No active subscription found for this customer"));
    }

    // ── Summary DTO ───────────────────────────────────────────────────────────

    @Data
    @Builder
    public static class AttendanceSummary {
        private Long customerId;
        private String customerName;
        private String memberCode;
        private String profileImageUrl;
        private Long subscriptionId;
        private String subscriptionStatus;
        private LocalDateTime visitedAt;
    }
}
