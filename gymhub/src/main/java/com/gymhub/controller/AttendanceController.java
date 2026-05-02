package com.gymhub.controller;

import com.gymhub.dto.request.RecordAttendanceRequest;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.service.AttendanceService;
import com.gymhub.service.AttendanceService.AttendanceSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gyms/{gymId}/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Record member and guest entry via barcode, dynamic QR or printed QR")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    @Operation(summary = "Record a member or guest visit. Returns member info for the receptionist's screen.")
    public ResponseEntity<AttendanceSummary> recordAttendance(
            @PathVariable Long gymId,
            @Valid @RequestBody RecordAttendanceRequest request,
            @RequestParam Long employeeId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.recordAttendance(gymId, request, employeeId));
    }

    @GetMapping
    @Operation(summary = "List all attendance records for this gym")
    public ResponseEntity<?> getGymAttendance(
            @PathVariable Long gymId,
            Pageable pageable) {
        return ResponseEntity.ok(
                PagedResponse.from(attendanceService.getGymAttendance(gymId, pageable)));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get attendance history for a specific customer")
    public ResponseEntity<?> getCustomerAttendance(
            @PathVariable Long gymId,
            @PathVariable Long customerId,
            Pageable pageable) {
        return ResponseEntity.ok(
                PagedResponse.from(attendanceService.getCustomerAttendance(customerId, pageable)));
    }
}
