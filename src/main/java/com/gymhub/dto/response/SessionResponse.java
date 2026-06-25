package com.gymhub.dto.response;

import com.gymhub.domain.session.SessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SessionResponse {
    private Long id;
    private Long gymId;
    private String title;
    private String description;
    private String instructorName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int capacity;
    private int bookedCount;
    private int availableSlots;
    private SessionStatus status;
}
