package com.gymhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InvitationResponse {
    private Long id;
    private Long gymId;
    private String gymName;
    private Long subscriptionId;
    private String guestFullName;
    private boolean guestRepeatVisitAllowed;
    private LocalDateTime usedAt;
}
