package com.gymhub.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FreezeRequest {

    @NotNull(message = "Freeze start date is required")
    @FutureOrPresent(message = "Freeze start date cannot be in the past")
    private LocalDate startDate;

    @NotNull(message = "Freeze end date is required")
    @Future(message = "Freeze end date must be in the future")
    private LocalDate endDate;

    @Size(max = 500)
    private String reason;
}
