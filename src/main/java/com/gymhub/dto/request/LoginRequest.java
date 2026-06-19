package com.gymhub.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    /**
     * Email or phone number. Existing clients may send this as "email" — both field names
     * are accepted via @JsonAlias for backward compatibility.
     */
    @NotBlank
    @JsonAlias("email")
    private String loginIdentifier;

    @NotBlank
    private String password;
}
