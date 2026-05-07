package com.gymhub.dto.response;

import com.gymhub.domain.attendance.AttendanceType;
import com.gymhub.domain.gym.EntranceMethod;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AttendanceResponse {
    private Long id;
    private Long gymId;
    private String gymName;
    private AttendanceType type;
    private EntranceMethod entranceMethod;
    private LocalDateTime visitedAt;
}
