package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.request.CashDayClosingRequest;
import com.gymhub.dto.response.CashDayClosingResponse;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.service.CashDayClosingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * GD-07 — daily cash closing for a gym.
 */
@RestController
@RequestMapping("/api/v1/gyms/{gymId}/cash-closings")
@RequiredArgsConstructor
@Tag(name = "Cash Day Closing", description = "Daily cash reconciliation (GD-07)")
public class CashDayClosingController {

    private final CashDayClosingService cashDayClosingService;

    @GetMapping("/preview")
    @Operation(summary = "Preview expected cash total for a business date without closing")
    public ResponseEntity<CashDayClosingResponse> preview(
            @PathVariable Long gymId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(cashDayClosingService.preview(gymId, date, currentUser));
    }

    @PostMapping
    @Operation(summary = "Close the cash day — requires CLOSE_CASH_DAY permission")
    public ResponseEntity<CashDayClosingResponse> close(
            @PathVariable Long gymId,
            @Valid @RequestBody CashDayClosingRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cashDayClosingService.closeDay(gymId, request, currentUser));
    }

    @GetMapping
    @Operation(summary = "List cash-day closing history (newest first)")
    public ResponseEntity<PagedResponse<CashDayClosingResponse>> history(
            @PathVariable Long gymId,
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(
                cashDayClosingService.getHistory(gymId, currentUser, pageable)));
    }
}
