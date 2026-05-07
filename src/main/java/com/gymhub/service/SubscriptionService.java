package com.gymhub.service;

import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.domain.gym.ActivationPolicy;
import com.gymhub.domain.gym.GymSettings;
import com.gymhub.domain.gympackage.GymPackage;
import com.gymhub.domain.subscription.ActivationType;
import com.gymhub.domain.subscription.Payment;
import com.gymhub.domain.subscription.PaymentMethod;
import com.gymhub.domain.subscription.Subscription;
import com.gymhub.domain.subscription.SubscriptionStatus;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.AddPaymentRequest;
import com.gymhub.dto.request.SellSubscriptionRequest;
import com.gymhub.dto.response.SubscriptionResponse;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.ResourceNotFoundException;
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
    private final GymAccessService gymAccessService;

    // ── Sell ──────────────────────────────────────────────────────────────────

    /**
     * Sell a subscription to a customer.
     * The acting user is resolved from JWT — never trusted from the request body.
     */
    @Transactional
    public SubscriptionResponse sellSubscription(Long gymId, SellSubscriptionRequest request,
                                                  User currentUser) {
        // Resolve acting employee from JWT (owner bypass, else requires SELL_SUBSCRIPTION)
        Employee seller = gymAccessService.resolveActingEmployee(
                currentUser, gymId, EmployeePermission.SELL_SUBSCRIPTION);

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

    /**
     * Record an additional payment instalment.
     * Requires the acting user to be the owner or any active employee of the gym.
     */
    @Transactional
    public SubscriptionResponse addPayment(Long gymId, Long subscriptionId,
                                           AddPaymentRequest request, User currentUser) {
        // Any active employee (or owner) can record a payment
        Employee emp = gymAccessService.assertDashboardAccess(currentUser, gymId);

        Subscription sub = findOrThrow(subscriptionId, gymId);

        if (request.getPaymentMethod() != null && request.getPaymentMethod() != PaymentMethod.CASH) {
            throw new BusinessException("Only CASH payments are accepted in Phase 1");
        }
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

        addPaymentInternal(sub, request.getAmount(), emp, request.getNotes());

        sub = subscriptionRepository.findById(subscriptionId).orElseThrow();

        GymSettings settings = settingsRepository.findByGymId(gymId).orElse(null);
        if (sub.isFullyPaid() && sub.getStatus() == SubscriptionStatus.PENDING_PAYMENT) {
            tryAutoActivate(sub, settings, null, emp);
        }

        return toResponse(subscriptionRepository.save(sub));
    }

    // ── Activation ────────────────────────────────────────────────────────────

    /**
     * Manually activate a subscription.
     * If the gym allows activation with remaining balance, partial payment is acceptable.
     * Requires ACTIVATE_SUBSCRIPTION permission (or gym owner).
     */
    @Transactional
    public SubscriptionResponse activateSubscription(Long gymId, Long subscriptionId,
                                                     User currentUser, LocalDate startDate) {
        Employee emp = gymAccessService.resolveActingEmployee(
                currentUser, gymId, EmployeePermission.ACTIVATE_SUBSCRIPTION);

        Subscription sub = findOrThrow(subscriptionId, gymId);

        if (sub.getStatus() != SubscriptionStatus.PENDING_ACTIVATION &&
            sub.getStatus() != SubscriptionStatus.PENDING_PAYMENT) {
            throw new BusinessException("Subscription cannot be activated in status: " + sub.getStatus());
        }

        if (!sub.isFullyPaid()) {
            GymSettings settings = settingsRepository.findByGymId(gymId).orElse(null);
            boolean allowPartialActivation = settings != null && settings.isAllowActivationWithRemainingBalance();
            if (!allowPartialActivation) {
                throw new BusinessException(
                        "Subscription has a remaining balance of " + sub.getRemainingAmount() +
                        ". Enable 'allowActivationWithRemainingBalance' in gym settings to activate with partial payment.");
            }
        }

        activate(sub, startDate != null ? startDate : LocalDate.now(), emp, ActivationType.MANUAL);
        return toResponse(subscriptionRepository.save(sub));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getByGym(Long gymId, User currentUser, Pageable pageable) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        return subscriptionRepository.findByGymId(gymId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getByCustomer(Long gymId, Long customerId,
                                                     User currentUser, Pageable pageable) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        return subscriptionRepository.findByCustomerId(customerId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(Long gymId, Long subscriptionId, User currentUser) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        return toResponse(findOrThrow(subscriptionId, gymId));
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void addPaymentInternal(Subscription sub, BigDecimal amount, Employee emp) {
        addPaymentInternal(sub, amount, emp, null);
    }

    private void addPaymentInternal(Subscription sub, BigDecimal amount, Employee emp, String notes) {
        Payment payment = Payment.builder()
                .subscription(sub)
                .amount(amount)
                .currency(sub.getCurrency())
                .paymentMethod(PaymentMethod.CASH)
                .receivedBy(emp)
                .notes(notes)
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
            activate(sub, deferredDate, emp, ActivationType.MANUAL);
            return;
        }
        if (settings == null || settings.getActivationPolicy() == ActivationPolicy.IMMEDIATE) {
            activate(sub, LocalDate.now(), emp, ActivationType.AUTOMATIC);
        } else {
            sub.setStatus(SubscriptionStatus.PENDING_ACTIVATION);
        }
    }

    private void activate(Subscription sub, LocalDate startDate, Employee emp, ActivationType activationType) {
        sub.setActivationDate(LocalDate.now());
        sub.setStartDate(startDate);
        int totalDays = sub.getGymPackage().getDurationDays() + sub.getGymPackage().getBonusDays();
        sub.setEndDate(startDate.plusDays(totalDays));
        sub.setActivatedBy(emp);
        sub.setActivationType(activationType);
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
                .usedFreezeDays(sub.getUsedFreezeDays())
                .remainingFreezeDays(sub.getRemainingFreezeDays())
                .activationType(sub.getActivationType())
                .soldByEmployeeName(sub.getSoldBy() != null ? sub.getSoldBy().getUser().getFullName() : null)
                .activatedByEmployeeName(sub.getActivatedBy() != null ? sub.getActivatedBy().getUser().getFullName() : null)
                .createdAt(sub.getCreatedAt())
                .build();
    }
}
