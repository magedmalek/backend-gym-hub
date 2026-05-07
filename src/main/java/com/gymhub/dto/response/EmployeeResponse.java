package com.gymhub.dto.response;

import com.gymhub.domain.employee.EmployeePermission;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class EmployeeResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String profileImageUrl;
    private Long gymId;
    private String gymName;
    private String jobTitle;
    private BigDecimal salary;
    private String salaryCurrency;
    private Set<EmployeePermission> permissions;
    private int hierarchyLevel;
    private boolean active;
    private LocalDateTime createdAt;
}
