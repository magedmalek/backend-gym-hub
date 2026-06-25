package com.gymhub.dto.response;

import com.gymhub.domain.session.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponse {
    private Long id;
    private Long sessionId;
    private String sessionTitle;
    private LocalDateTime sessionStartTime;
    private Long customerId;
    private String customerName;
    private BookingStatus status;
    private LocalDateTime bookedAt;
    private LocalDateTime cancelledAt;
}
