package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.response.GymReportResponse;
import com.gymhub.service.GymReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * GD-15 — operational reports for a gym dashboard.
 */
@RestController
@RequestMapping("/api/v1/gyms/{gymId}/reports")
@RequiredArgsConstructor
@Tag(name = "Reports & Analytics", description = "Gym operational summaries (GD-15)")
public class GymReportController {

    private final GymReportService gymReportService;

    @GetMapping("/summary")
    @Operation(summary = "Revenue, subscription, customer and attendance summary for a date range "
            + "(defaults to current month-to-date)")
    public ResponseEntity<GymReportResponse> summary(
            @PathVariable Long gymId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(gymReportService.getSummary(gymId, from, to, currentUser));
    }
}
