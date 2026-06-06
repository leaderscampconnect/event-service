package com.awd2026.eventservice.dto;

public record RegistrationResponse(
        String participantId,
        RegistrationStatus registrationStatus,
        int waitlistPosition,
        EventResponse event
) {
    public enum RegistrationStatus {
        CONFIRMED,
        WAITLISTED
    }
}

