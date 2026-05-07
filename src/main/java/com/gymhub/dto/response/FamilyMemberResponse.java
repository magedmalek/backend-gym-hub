package com.gymhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FamilyMemberResponse {
    private Long id;
    private Long mainCustomerId;
    private Long subCustomerId;
    private String subCustomerFullName;
    private String subCustomerPhone;
    private Long subscriptionId;
    private String relationType;
    private String createdByEmployeeName;
    private LocalDateTime createdAt;
}
