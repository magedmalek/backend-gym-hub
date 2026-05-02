package com.gymhub.dto.response;

import com.gymhub.domain.gym.GymStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GymResponse {
    private Long id;
    private String name;
    private String description;
    private String logoUrl;
    private String address;
    private String city;
    private String country;
    private String phone;
    private String email;
    private String website;
    private GymStatus status;
    private Long ownerId;
    private String ownerName;
    private LocalDateTime createdAt;
}
