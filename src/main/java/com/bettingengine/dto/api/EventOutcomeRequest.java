package com.bettingengine.dto.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EventOutcomeRequest(
        @NotNull @Positive Long eventId,
        @NotBlank String eventName,
        @NotNull @Positive Long eventWinnerId
) {
}
