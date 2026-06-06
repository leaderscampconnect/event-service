package com.awd2026.eventservice.dto;

public record EventAvailabilityResponse(
        String eventId,
        int capacity,
        int registeredCount,
        int availableSeats,
        int waitlistCount,
        int waitlistCapacity,
        boolean fullyBooked,
        double occupancyRate
) {
}

