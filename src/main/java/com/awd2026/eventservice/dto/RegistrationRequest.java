package com.awd2026.eventservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank @Size(max = 120) String participantId
) {
}

