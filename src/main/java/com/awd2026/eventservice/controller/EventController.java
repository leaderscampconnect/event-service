package com.awd2026.eventservice.controller;

import com.awd2026.eventservice.client.NotificationClient;
import com.awd2026.eventservice.dto.CancelEventRequest;
import com.awd2026.eventservice.dto.EventAvailabilityResponse;
import com.awd2026.eventservice.dto.EventRequest;
import com.awd2026.eventservice.dto.EventResponse;
import com.awd2026.eventservice.dto.PostponeEventRequest;
import com.awd2026.eventservice.dto.RegistrationRequest;
import com.awd2026.eventservice.dto.RegistrationResponse;
import com.awd2026.eventservice.entity.EventCategory;
import com.awd2026.eventservice.entity.EventStatus;
import com.awd2026.eventservice.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events")
@Tag(name = "Events", description = "Event lifecycle, registration, and availability")
public class EventController {

    private final EventService eventService;
    private final NotificationClient notificationClient;

    public EventController(EventService eventService, NotificationClient notificationClient) {
        this.eventService = eventService;
        this.notificationClient = notificationClient;
    }

    @GetMapping
    @Operation(summary = "List and filter events")
    public List<EventResponse> getEvents(
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean published
    ) {
        return eventService.findEvents(category, status, location, published);
    }

    @GetMapping("/search")
    @Operation(summary = "Search events by title, description, or location")
    public List<EventResponse> searchEvents(@RequestParam String keyword) {
        return eventService.searchEvents(keyword);
    }

    @GetMapping("/upcoming")
    @Operation(summary = "List upcoming published events")
    public List<EventResponse> getUpcomingEvents() {
        return eventService.getUpcomingEvents();
    }

    @GetMapping("/available")
    @Operation(summary = "List events accepting registrations or waitlist entries")
    public List<EventResponse> getAvailableEvents() {
        return eventService.getAvailableEvents();
    }

    @GetMapping("/with-notification")
    public EventWithNotificationResponse getEventsWithNotification() {
        return new EventWithNotificationResponse(
                eventService.findEvents(null, null, null, null),
                notificationClient.getNotifications()
        );
    }

    @GetMapping("/{id}")
    public EventResponse getEvent(@PathVariable String id) {
        return eventService.getEvent(id);
    }

    @PostMapping
    @Operation(summary = "Create an event")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createEvent(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an editable event")
    public EventResponse updateEvent(
            @PathVariable String id,
            @Valid @RequestBody EventRequest request
    ) {
        return eventService.updateEvent(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/publish")
    public EventResponse publishEvent(@PathVariable String id) {
        return eventService.publishEvent(id);
    }

    @PatchMapping("/{id}/unpublish")
    public EventResponse unpublishEvent(@PathVariable String id) {
        return eventService.unpublishEvent(id);
    }

    @PostMapping("/{id}/registrations")
    @Operation(summary = "Register a participant or add them to the waitlist")
    public RegistrationResponse registerParticipant(
            @PathVariable String id,
            @Valid @RequestBody RegistrationRequest request
    ) {
        return eventService.registerParticipant(id, request.participantId());
    }

    @DeleteMapping("/{id}/registrations/{participantId}")
    public EventResponse cancelRegistration(
            @PathVariable String id,
            @PathVariable String participantId
    ) {
        return eventService.cancelRegistration(id, participantId);
    }

    @PatchMapping("/{id}/postpone")
    @Operation(summary = "Postpone an event and notify participants")
    public EventResponse postponeEvent(
            @PathVariable String id,
            @Valid @RequestBody PostponeEventRequest request
    ) {
        return eventService.postponeEvent(id, request);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an event and notify participants")
    public EventResponse cancelEvent(
            @PathVariable String id,
            @Valid @RequestBody CancelEventRequest request
    ) {
        return eventService.cancelEvent(id, request);
    }

    @PatchMapping("/{id}/status")
    public EventResponse changeStatus(
            @PathVariable String id,
            @RequestParam EventStatus status
    ) {
        return eventService.changeStatus(id, status);
    }

    @GetMapping("/{id}/availability")
    public EventAvailabilityResponse getAvailability(@PathVariable String id) {
        return eventService.getAvailability(id);
    }

    @GetMapping("/notifications")
    public List<NotificationClient.NotificationResponse> getNotifications() {
        return notificationClient.getNotifications();
    }

    public record EventWithNotificationResponse(
            List<EventResponse> events,
            List<NotificationClient.NotificationResponse> notifications
    ) {
    }
}
