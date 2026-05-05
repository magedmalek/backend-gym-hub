package com.gymhub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateGymRequest {

    @NotBlank(message = "Gym name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 1000)
    private String description;

    @Size(max = 200)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String country;

    @Size(max = 20)
    private String phone;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 200)
    private String website;
}
