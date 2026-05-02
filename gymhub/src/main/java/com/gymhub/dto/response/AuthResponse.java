package com.gymhub.dto.response;

import com.gymhub.domain.user.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String email;
    private String fullName;
    private Set<UserRole> roles;
    private UserRole activeContext;
}
