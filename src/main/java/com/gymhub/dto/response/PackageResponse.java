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
    private BigDecimal price;
    private String currency;
    private int maxInvitations;
    private boolean allowGuestRepeatVisit;
    private boolean allowPartialPayment;
    private boolean active;
    private List<ServiceResponse> includedServices;
    private LocalDateTime createdAt;
}
