package com.gymhub.dto.request;

import com.gymhub.domain.gym.ActivationPolicy;
import com.gymhub.domain.gym.EntranceMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class GymSettingsRequest {

    private boolean allowPartialPayment;

    @NotNull
    private ActivationPolicy activationPolicy;

    /** Allow activating a subscription even when there is a remaining balance. */
    private boolean allowActivationWithRemainingBalance;

    private Set<EntranceMethod> enabledEntranceMethods;
}
