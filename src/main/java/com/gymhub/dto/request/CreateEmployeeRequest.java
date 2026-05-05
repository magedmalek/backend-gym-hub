package com.gymhub.dto.request;

import com.gymhub.domain.employee.EmployeePermission;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class CreateEmployeeRequest {

    @NotBlank
    @Email
    private String userEmail;         // existing platform user OR new user to register

    @NotBlank
    @Size(max = 100)
    private String jobTitle;

    @DecimalMin("0.0")
    private BigDecimal salary;

    @Size(max = 10)
    private String salaryCurrency = "EGP";

    @Size(max = 500)
    private String notes;

    private Set<EmployeePermission> permissions;

    // ── Fields used when creating a NEW user for this employee ───────────────
    private String firstName;
    private String lastName;
    private String phone;
    private String password;
}
