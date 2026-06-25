package com.gymhub.dto.request;

import com.gymhub.domain.request.ComplaintType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Customer request to file a complaint. POST /api/v1/customer/complaints.
 * At least one of gymId / againstProviderId should be supplied for routing.
 */
@Data
public class ComplaintRequest {

    @NotNull(message = "Complaint type is required")
    private ComplaintType type;

    @NotBlank(message = "Subject is required")
    @Size(max = 200)
    private String subject;

    @Size(max = 2000)
    private String body;

    private Long gymId;

    private Long againstProviderId;
}
