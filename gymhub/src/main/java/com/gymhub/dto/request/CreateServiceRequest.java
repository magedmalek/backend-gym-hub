package com.gymhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateServiceRequest {

    @NotBlank(message = "Service name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String description;

    private boolean canBeIncludedInPackage = true;

    private boolean canBeSoldIndependently = false;
}
