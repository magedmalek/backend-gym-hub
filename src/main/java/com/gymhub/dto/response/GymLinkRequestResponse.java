package com.gymhub.dto.response;

import com.gymhub.domain.customer.GymLinkRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GymLinkRequestResponse {
    private Long id;
    private Long gymId;
    private String gymName;
    private GymLinkRequestStatus status;
    private String notes;
    private LocalDateTime requestedAt;
    private LocalDateTime resolvedAt;
}
