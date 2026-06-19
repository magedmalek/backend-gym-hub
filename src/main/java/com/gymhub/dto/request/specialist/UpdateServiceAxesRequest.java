package com.gymhub.dto.request.specialist;

import com.gymhub.domain.specialist.ServiceAxis;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateServiceAxesRequest {

    @NotNull(message = "Main service axis is required")
    private ServiceAxis mainAxis;

    @NotEmpty(message = "At least one service axis must be selected")
    private List<ServiceAxis> selectedAxes;
}
