package com.awd2026.eventservice.dto;

import com.awd2026.eventservice.entity.EventCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventRequest(
        @NotBlank @Size(min = 3, max = 160) String title,
        @NotBlank @Size(min = 10, max = 2000) String description,
        @NotNull EventCategory category,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @NotBlank @Size(max = 255) String location,
        @NotBlank @Size(max = 120) String organizerId,
        @Min(1) @Max(10000) int capacity,
        @Min(0) @Max(1000) int waitlistCapacity,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        boolean published
) {
}

