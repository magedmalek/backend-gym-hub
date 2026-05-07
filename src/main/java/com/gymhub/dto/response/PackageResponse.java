package com.gymhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PackageResponse {
    private Long id;
    private Long gymId;
    private String name;
    private String description;
    private int durationDays;
    private int bonusDays;
    private BigDecimal price;
    private String currency;
    private int freezeAllowanceDays;
    private int maxInvitations;
    private boolean allowGuestRepeatVisit;
    private boolean allowPartialPayment;
    private boolean isFamilyPackage;
    private int maxSubUsers;
    private boolean active;
    private List<ServiceResponse> includedServices;
    private LocalDateTime createdAt;
}
