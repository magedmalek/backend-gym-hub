package com.gymhub.dto.response.specialist;

import com.gymhub.domain.specialist.ServiceAxis;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ServiceAxisSelectionResponse {

    private Long id;
    private ServiceAxis serviceAxis;
    private boolean isMainAxis;
    private boolean active;
    private LocalDateTime selectedAt;
}
