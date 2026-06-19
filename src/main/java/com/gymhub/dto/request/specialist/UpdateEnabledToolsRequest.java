package com.gymhub.dto.request.specialist;

import com.gymhub.domain.specialist.ToolCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateEnabledToolsRequest {

    @NotNull
    private List<ToolToggle> tools;

    @Data
    public static class ToolToggle {
        @NotNull
        private ToolCode toolCode;
        private boolean enabled;
        @Size(max = 500)
        private String disableReason;
    }
}
