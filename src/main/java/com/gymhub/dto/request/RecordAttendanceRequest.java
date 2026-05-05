package com.gymhub.dto.request;

import com.gymhub.domain.attendance.AttendanceType;
import com.gymhub.domain.gym.EntranceMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecordAttendanceRequest {

    /** Member code (barcode / QR value) or customer ID — at least one is required. */
    private String memberCode;

    private Long customerId;

    @NotNull
    private EntranceMethod entranceMethod;

    @NotNull
    private AttendanceType type;

    /** For MEMBER_VISIT: active subscription to link to (optional; service resolves it automatically). */
    private Long subscriptionId;
}
