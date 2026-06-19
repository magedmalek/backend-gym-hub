package com.gymhub.dto.response.specialist;

import com.gymhub.domain.specialist.ServiceAxis;
import com.gymhub.domain.specialist.ToolCode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnabledToolResponse {

    private Long id;
    private ToolCode toolCode;
    private ServiceAxis serviceAxis;
    private boolean enabled;
    private boolean autoEnabled;
    private boolean disabledBySpecialist;
    private String disableReason;
    /** When true the frontend should display a service-completeness warning. */
    private boolean warningRequired;
}
