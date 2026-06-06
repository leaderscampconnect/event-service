package com.awd2026.eventservice.dto;

import com.awd2026.eventservice.entity.Event;
import com.awd2026.eventservice.entity.EventCategory;
import com.awd2026.eventservice.entity.EventStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record EventResponse(
        String id,
        String title,
        String description,
        EventCategory category,
        EventStatus status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String location,
        String organizerId,
        int capacity,
        int waitlistCapacity,
        BigDecimal price,
        boolean published,
        int registeredCount,
        int waitlistCount,
        int availableSeats,
        boolean fullyBooked,
        double occupancyRate,
        Set<String> participantIds,
        Set<String> waitlistParticipantIds,
        String cancellationReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getStatus(),
                event.getStartAt(),
                event.getEndAt(),
                event.getLocation(),
                event.getOrganizerId(),
                event.getCapacity(),
                event.getWaitlistCapacity(),
                event.getPrice(),
                event.isPublished(),
                event.getParticipantIds().size(),
                event.getWaitlistParticipantIds().size(),
                event.getAvailableSeats(),
                event.isFullyBooked(),
                event.getOccupancyRate(),
                Set.copyOf(event.getParticipantIds()),
                Set.copyOf(event.getWaitlistParticipantIds()),
                event.getCancellationReason(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}

