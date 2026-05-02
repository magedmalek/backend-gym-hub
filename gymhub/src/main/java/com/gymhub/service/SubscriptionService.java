package com.gymhub.service;

import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.domain.gym.ActivationPolicy;
import com.gymhub.domain.gym.GymSettings;
import com.gymhub.domain.gympackage.GymPackage;
import com.gymhub.domain.subscription.Payment;
import com.gymhub.domain.subscription.Subscription;
import com.gymhub.domain.subscription.SubscriptionStatus;
import com.gymhub.dto.request.AddPaymentRequest;
import com.gymhub.dto.request.SellSubscriptionRequest;
import com.gymhub.dto.response.SubscriptionResponse;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.exception.UnauthorizedException;
import com.gymhub.repository.GymSettingsRepository;
import com.gymhub.repository.PaymentRepository;
import com.gymhub.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final GymSettingsRepository settingsRepository;
    private final CustomerService customerService;
    private final PackageService packageService;
    private final EmployeeManagementService employeeService;

    // ── Sell ──────────────────────────────────────────────────────────────────

    @Transactional
    public SubscriptionResponse sellSubscription(Long gymId, SellSubscriptionRequest request,
                                                   Long sellerEmployeeId) {
        Employee seller = employeeService.findOrThrow(sellerEmployeeId);
        if (!seller.hasPermission(EmployeePermission.SELL_SUBSCRIPTION)) {
            throw new UnauthorizedException("Employee does not have permission to sell subscriptions");
        }

        Customer customer = customerService.findOrThrow(request.getCustomerId());
        GymPackage pkg    = packageService.findOrThrow(request.getPackageId());

        if (!pkg.getGym().getId().equals(gymId)) {
            throw new BusinessException("Package does not belong to this gym");
        }
        if (!pkg.isActive()) {
            throw new BusinessException("Package is not active");
        }

        GymSettings settings = settingsRepository.findByGymId(gymId).orElse(null);

        // Validate partial payment
        boolean partialAllowed = pkg.isAllowPartialPayment() ||
                (settings != null && settings.isAllowPartialPayment());
        BigDecimal initialPayment = request.getInitialPaymentAmount();

        if (!partialAllowed && initialPayment.compareTo(pkg.getPrice()) < 0) {
            throw new BusinessException("Partial payment is not allowed for this package");
        }
        if (initialPayment.compareTo(pkg.getPrice()) > 0) {
            throw new BusinessException("Payment exceeds total price");
        }

        Subscription sub = Subscription.builder()
                .customer(customer)
                .gymPackage(pkg)
                .gym(pkg.getGym())
                .soldBy(seller)
                .totalPrice(pkg.getPrice())
                .paidAmount(BigDecimal.ZERO)
                .currency(pkg.getCurrency())
                .saleDate(LocalDate.now())
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .build();

        sub = subscriptionRepository.save(sub);

        // Record initial payment
        addPaymentInternal(sub, initialPayment, seller);

        // Refresh the saved sub
        sub = subscriptionRepository.findById(sub.getId()).orElseThrow();

        // Auto-activate if conditions are met
        if (sub.isFullyPaid()) {
            tryAutoActivate(sub, settings, request.getDeferredStartDate(), seller);
        }

        return toResponse(subscriptionRepository.save(sub));
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    @Transactional
    public SubscriptionResponse addPayment(Long gymId, Long subscriptionId,
                                            AddPaymentRequest request, Long employeeId) {
        Subscription sub = findOrThrow(subscriptionId, gymId);
        Employee emp = employeeService.findOrThrow(employeeId);

        if (sub.getStatus() == SubscriptionStatus.CANCELLED ||
            sub.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new BusinessException("Cannot add payment to a " + sub.getStatus() + " subscription");
        }
        if (sub.isFullyPaid()) {
            throw new BusinessException("Subscription is already fully paid");
        }

        BigDecimal remaining = sub.getRemainingAmount();
        if (request.getAmount().compareTo(remaining) > 0) {
            throw new BusinessException("Payment exceeds remaining balance of " + remaining);
        }

        addPaymentInternal(sub, request.getAmount(), emp);

        sub = subscriptionRepository.findById(subscriptionId).orElseThrow();

        GymSettings settings = settingsRepository.findByGymId(gymId).orElse(null);
        if (sub.isFullyPaid() && sub.getStatus() == SubscriptionStatus.PENDING_PAYMENT) {
            tryAutoActivate(sub, settings, null, emp);
        }

        return toResponse(subscriptionRepository.save(sub));
    }

    // ── Activation ────────────────────────────────────────────────────────────

    @Transactional
    public SubscriptionResponse activateSubscription(Long gymId, Long subscriptionId,
                                                      Long employeeId, LocalDate startDate) {
        Subscription sub = findOrThrow(subscriptionId, gymId);
        Employee emp = employeeService.findOrThrow(employeeId);

        if (!emp.hasPermission(EmployeePermission.ACTIVATE_SUBSCRIPTION)) {
            throw new UnauthorizedException("Employee does not have permission to activate subscriptions");
        }
        if (sub.getStatus() != SubscriptionStatus.PENDING_ACTIVATION &&
            sub.getStatus() != SubscriptionStatus.PENDING_PAYMENT) {
            throw new BusinessException("Subscription cannot be activated in status: " + sub.getStatus());
        }
        if (!sub.isFullyPaid()) {
            throw new BusinessException("Subscription is not fully paid yet");
        }

        activate(sub, startDate != null ? startDate : LocalDate.now(), emp);
        return toResponse(subscriptionRepository.save(sub));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getByGym(Long gymId, Pageable pageable) {
        return subscriptionRepository.findByGymId(gymId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getByCustomer(Long customerId, Pageable pageable) {
        return subscriptionRepository.findByCustomerId(customerId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(Long gymId, Long subscriptionId) {
        return toResponse(findOrThrow(subscriptionId, gymId));
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void addPaymentInternal(Subscription sub, BigDecimal amount, Employee emp) {
        Payment payment = Payment.builder()
                .subscription(sub)
                .amount(amount)
                .currency(sub.getCurrency())
                .receivedBy(emp)
                .build();
        paymentRepository.save(payment);

        sub.setPaidAmount(sub.getPaidAmount().add(amount));
        if (sub.getStatus() == SubscriptionStatus.PENDING_PAYMENT && sub.isFullyPaid()) {
            sub.setStatus(SubscriptionStatus.PENDING_ACTIVATION);
        }
        subscriptionRepository.save(sub);
    }

    private void tryAutoActivate(Subscription sub, GymSettings settings,
                                  LocalDate deferredDate, Employee emp) {
        if (deferredDate != null) {
            // Deferred start = activation happens now but validity starts on the deferred date
            activate(sub, deferredDate, emp);
            return;
        }
        if (settings == null || settings.getActivationPolicy() == ActivationPolicy.IMMEDIATE) {
            activate(sub, LocalDate.now(), emp);
        } else {
            sub.setStatus(SubscriptionStatus.PENDING_ACTIVATION);
        }
    }

    private void activate(Subscription sub, LocalDate startDate, Employee emp) {
        sub.setActivationDate(LocalDate.now());
        sub.setStartDate(startDate);
        sub.setEndDate(startDate.plusDays(sub.getGymPackage().getDurationDays()));
        sub.setActivatedBy(emp);
        sub.setStatus(SubscriptionStatus.ACTIVE);
    }

    public Subscription findOrThrow(Long subscriptionId, Long gymId) {
        return subscriptionRepository.findByIdAndGymId(subscriptionId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", subscriptionId));
    }

    private SubscriptionResponse toResponse(Subscription sub) {
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
                .soldByEmployeeName(sub.getSoldBy() != null ? sub.getSoldBy().getUser().getFullName() : null)
                .activatedByEmployeeName(sub.getActivatedBy() != null ? sub.getActivatedBy().getUser().getFullName() : null)
                .createdAt(sub.getCreatedAt())
                .build();
    }
}
