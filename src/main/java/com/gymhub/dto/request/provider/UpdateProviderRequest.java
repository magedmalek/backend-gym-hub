package com.gymhub.dto.request.provider;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProviderRequest {

    @Size(max = 200)
    private String displayName;

    @Size(max = 20)
    private String phone;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 500)
    private String logoUrl;

    @Size(max = 500)
    private String coverImageUrl;
}
