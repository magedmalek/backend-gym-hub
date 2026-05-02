package com.gymhub.dto.response;

import com.gymhub.domain.subscription.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long packageId;
    private String packageName;
    private Long gymId;
    private BigDecimal totalPrice;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private String currency;
    private SubscriptionStatus status;
    private LocalDate saleDate;
    private LocalDate activationDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private int maxInvitations;
    private int usedInvitations;
    private int remainingInvitations;
    private String soldByEmployeeName;
    private String activatedByEmployeeName;
    private LocalDateTime createdAt;
}
