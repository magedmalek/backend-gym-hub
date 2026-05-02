package com.gymhub.dto.response;

import com.gymhub.domain.gymservice.ServiceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ServiceResponse {
    private Long id;
    private Long gymId;
    private String name;
    private String description;
    private boolean canBeIncludedInPackage;
    private boolean canBeSoldIndependently;
    private ServiceStatus status;
    private LocalDateTime createdAt;
}
