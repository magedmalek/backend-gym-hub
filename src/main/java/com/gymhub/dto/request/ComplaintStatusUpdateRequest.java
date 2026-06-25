package com.gymhub.dto.request;

import com.gymhub.domain.request.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Dashboard staff updating a complaint's status / resolution.
 */
@Data
public class ComplaintStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private ComplaintStatus status;

    @Size(max = 2000)
    private String resolutionNote;
}
