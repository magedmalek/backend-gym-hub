package com.gymhub.dto.response;

import com.gymhub.domain.freeze.FreezeStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class FreezeResponse {
    private Long id;
    private Long subscriptionId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate actualEndDate;
    private Integer daysConsumed;
    private String reason;
    private FreezeStatus status;
    private String registeredByEmployeeName;
    private String cancelledByEmployeeName;
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;
}
