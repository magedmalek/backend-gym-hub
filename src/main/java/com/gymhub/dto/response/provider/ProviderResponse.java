package com.gymhub.dto.response.provider;

import com.gymhub.domain.provider.ProviderStatus;
import com.gymhub.domain.provider.ProviderType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProviderResponse {

    private Long id;
    private ProviderType providerType;
    private Long ownerUserId;
    private String ownerName;
    private Long linkedGymId;
    private String displayName;
    private String phone;
    private String email;
    private String logoUrl;
    private String coverImageUrl;
    private ProviderStatus status;
    private boolean active;
    private LocalDateTime createdAt;
}
