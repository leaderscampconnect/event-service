package com.awd2026.eventservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank
        @Size(max = 20)
        @Pattern(
                regexp = "^[0-9]+$",
                message = "Participant ID must be a numeric user-service identifier"
        )
        String participantId
) {
}

