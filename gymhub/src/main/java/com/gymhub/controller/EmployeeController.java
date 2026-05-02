package com.gymhub.controller;

import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.dto.request.CreateEmployeeRequest;
import com.gymhub.dto.response.EmployeeResponse;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.service.EmployeeManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/gyms/{gymId}/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Management", description = "Manage gym employees, their roles, salaries and permissions")
public class EmployeeController {

    private final EmployeeManagementService employeeService;

    @PostMapping
    @Operation(summary = "Add a new employee to the gym")
    public ResponseEntity<EmployeeResponse> createEmployee(
            @PathVariable Long gymId,
            @Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.createEmployee(gymId, request));
    }

    @GetMapping
    @Operation(summary = "List all employees of the gym")
    public ResponseEntity<PagedResponse<EmployeeResponse>> getEmployees(
            @PathVariable Long gymId,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(employeeService.getEmployees(gymId, pageable)));
    }

    @GetMapping("/{employeeId}")
    @Operation(summary = "Get a specific employee's details")
    public ResponseEntity<EmployeeResponse> getEmployee(
            @PathVariable Long gymId,
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.getEmployee(gymId, employeeId));
    }

    @PutMapping("/{employeeId}/permissions")
    @Operation(summary = "Update the permissions assigned to an employee")
    public ResponseEntity<EmployeeResponse> updatePermissions(
            @PathVariable Long gymId,
            @PathVariable Long employeeId,
            @RequestBody Set<EmployeePermission> permissions) {
        return ResponseEntity.ok(employeeService.updatePermissions(gymId, employeeId, permissions));
    }

    @PatchMapping("/{employeeId}/status")
    @Operation(summary = "Activate or deactivate an employee")
    public ResponseEntity<Void> toggleStatus(
            @PathVariable Long gymId,
            @PathVariable Long employeeId,
            @RequestParam boolean active) {
        employeeService.toggleStatus(gymId, employeeId, active);
        return ResponseEntity.noContent().build();
    }
}
