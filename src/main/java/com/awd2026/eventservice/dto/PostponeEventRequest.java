package com.awd2026.eventservice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PostponeEventRequest(
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt
) {
}

