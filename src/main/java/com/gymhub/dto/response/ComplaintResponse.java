package com.gymhub.dto.response;

import com.gymhub.domain.request.ComplaintStatus;
import com.gymhub.domain.request.ComplaintType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ComplaintResponse {
    private Long id;
    private String complainantName;
    private ComplaintType type;
    private String subject;
    private String body;
    private Long gymId;
    private Long againstProviderId;
    private ComplaintStatus status;
    private String resolutionNote;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
