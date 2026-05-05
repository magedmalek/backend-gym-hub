package com.gymhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CustomerResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String profileImageUrl;
    private Long gymId;
    private String gymName;
    private String memberCode;
    private boolean active;
    private LocalDateTime joinedAt;
}
