package com.awd2026.eventservice.service;

import com.awd2026.eventservice.client.NotificationClient;
import com.awd2026.eventservice.dto.CancelEventRequest;
import com.awd2026.eventservice.dto.EventAvailabilityResponse;
import com.awd2026.eventservice.dto.EventRequest;
import com.awd2026.eventservice.dto.EventResponse;
import com.awd2026.eventservice.dto.PostponeEventRequest;
import com.awd2026.eventservice.dto.RegistrationResponse;
import com.awd2026.eventservice.entity.Event;
import com.awd2026.eventservice.entity.EventCategory;
import com.awd2026.eventservice.entity.EventStatus;
import com.awd2026.eventservice.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

@Service
public class EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final NotificationClient notificationClient;

    public EventService(EventRepository eventRepository, NotificationClient notificationClient) {
        this.eventRepository = eventRepository;
        this.notificationClient = notificationClient;
    }

    public List<EventResponse> findEvents(
            EventCategory category,
            EventStatus status,
            String location,
            Boolean published
    ) {
        return eventRepository.findAll(Sort.by(Sort.Direction.ASC, "startAt")).stream()
                .filter(event -> category == null || category == event.getCategory())
                .filter(event -> status == null || status == event.getStatus())
                .filter(event -> location == null || location.isBlank()
                        || containsIgnoreCase(event.getLocation(), location))
                .filter(event -> published == null || published == event.isPublished())
                .map(EventResponse::from)
                .toList();
    }

    public List<EventResponse> searchEvents(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return eventRepository.findAll(Sort.by(Sort.Direction.ASC, "startAt")).stream()
                .filter(event -> containsIgnoreCase(event.getTitle(), keyword)
                        || containsIgnoreCase(event.getDescription(), keyword)
                        || containsIgnoreCase(event.getLocation(), keyword))
                .map(EventResponse::from)
                .toList();
    }

    public List<EventResponse> getUpcomingEvents() {
        LocalDateTime now = LocalDateTime.now();
        return eventRepository.findAll(Sort.by(Sort.Direction.ASC, "startAt")).stream()
                .filter(Event::isPublished)
                .filter(event -> event.getStartAt() != null && event.getStartAt().isAfter(now))
                .filter(event -> event.getStatus() == EventStatus.SCHEDULED
                        || event.getStatus() == EventStatus.POSTPONED)
                .map(EventResponse::from)
                .toList();
    }

    public List<EventResponse> getAvailableEvents() {
        return getUpcomingEvents().stream()
                .filter(event -> event.availableSeats() > 0
                        || event.waitlistCount() < event.waitlistCapacity())
                .toList();
    }

    public EventResponse getEvent(String id) {
        return EventResponse.from(getEventEntity(id));
    }

    public EventResponse createEvent(EventRequest request) {
        validateSchedule(request.startAt(), request.endAt());
        LocalDateTime now = LocalDateTime.now();

        Event event = new Event();
        applyRequest(event, request);
        event.setStatus(request.published() ? EventStatus.SCHEDULED : EventStatus.DRAFT);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);

        Event savedEvent = eventRepository.save(event);
        sendNotification(
                savedEvent.getOrganizerId(),
                savedEvent,
                "EVENT_CREATED",
                "Event created",
                savedEvent.getTitle() + " was created successfully."
        );
        return EventResponse.from(savedEvent);
    }

    public EventResponse updateEvent(String id, EventRequest request) {
        Event event = getEventEntity(id);
        ensureEditable(event);
        validateSchedule(request.startAt(), request.endAt());
        if (request.capacity() < event.getParticipantIds().size()) {
            throw badRequest("Capacity cannot be lower than confirmed registrations");
        }
        if (request.waitlistCapacity() < event.getWaitlistParticipantIds().size()) {
            throw badRequest("Waitlist capacity cannot be lower than the current waitlist");
        }

        applyRequest(event, request);
        if (event.getStatus() == EventStatus.DRAFT && request.published()) {
            event.setStatus(EventStatus.SCHEDULED);
        } else if (!request.published() && event.getStatus() == EventStatus.SCHEDULED) {
            ensureNoRegistrations(event, "An event with registrations cannot return to draft");
            event.setStatus(EventStatus.DRAFT);
        }
        event.setUpdatedAt(LocalDateTime.now());
        return EventResponse.from(eventRepository.save(event));
    }

    public void deleteEvent(String id) {
        Event event = getEventEntity(id);
        if (event.getStatus() == EventStatus.ONGOING
                || event.getStatus() == EventStatus.COMPLETED) {
            throw conflict("Ongoing or completed events cannot be deleted");
        }
        ensureNoRegistrations(event, "Cancel events with registrations instead of deleting them");
        eventRepository.delete(event);
    }

    public EventResponse publishEvent(String id) {
        Event event = getEventEntity(id);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw conflict("Only draft events can be published");
        }
        event.setPublished(true);
        event.setStatus(EventStatus.SCHEDULED);
        event.setUpdatedAt(LocalDateTime.now());
        return EventResponse.from(eventRepository.save(event));
    }

    public EventResponse unpublishEvent(String id) {
        Event event = getEventEntity(id);
        if (event.getStatus() != EventStatus.SCHEDULED) {
            throw conflict("Only scheduled events can return to draft");
        }
        ensureNoRegistrations(event, "An event with registrations cannot return to draft");
        event.setPublished(false);
        event.setStatus(EventStatus.DRAFT);
        event.setUpdatedAt(LocalDateTime.now());
        return EventResponse.from(eventRepository.save(event));
    }

    public RegistrationResponse registerParticipant(String id, String participantId) {
        Event event = getEventEntity(id);
        String normalizedParticipantId = participantId.trim();
        ensureRegistrationOpen(event);

        if (event.getParticipantIds().contains(normalizedParticipantId)
                || event.getWaitlistParticipantIds().contains(normalizedParticipantId)) {
            throw conflict("Participant is already registered or waitlisted");
        }

        RegistrationResponse.RegistrationStatus registrationStatus;
        int waitlistPosition = 0;
        String notificationType;
        String notificationTitle;
        String notificationMessage;

        if (event.getAvailableSeats() > 0) {
            event.getParticipantIds().add(normalizedParticipantId);
            registrationStatus = RegistrationResponse.RegistrationStatus.CONFIRMED;
            notificationType = "REGISTRATION_CONFIRMED";
            notificationTitle = "Registration confirmed";
            notificationMessage = "Your registration for " + event.getTitle() + " is confirmed.";
        } else if (event.getWaitlistParticipantIds().size() < event.getWaitlistCapacity()) {
            event.getWaitlistParticipantIds().add(normalizedParticipantId);
            registrationStatus = RegistrationResponse.RegistrationStatus.WAITLISTED;
            waitlistPosition = event.getWaitlistParticipantIds().size();
            notificationType = "WAITLIST_JOINED";
            notificationTitle = "Added to waitlist";
            notificationMessage = "You joined the waitlist for " + event.getTitle() + ".";
        } else {
            throw conflict("Event and waitlist are full");
        }

        event.setUpdatedAt(LocalDateTime.now());
        Event savedEvent = eventRepository.save(event);
        sendNotification(
                normalizedParticipantId,
                savedEvent,
                notificationType,
                notificationTitle,
                notificationMessage
        );

        return new RegistrationResponse(
                normalizedParticipantId,
                registrationStatus,
                waitlistPosition,
                EventResponse.from(savedEvent)
        );
    }

    public EventResponse cancelRegistration(String id, String participantId) {
        Event event = getEventEntity(id);
        String normalizedParticipantId = participantId.trim();
        boolean removedFromConfirmed = event.getParticipantIds().remove(normalizedParticipantId);
        boolean removedFromWaitlist = event.getWaitlistParticipantIds().remove(normalizedParticipantId);

        if (!removedFromConfirmed && !removedFromWaitlist) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Registration not found for participant: " + normalizedParticipantId
            );
        }

        sendNotification(
                normalizedParticipantId,
                event,
                "REGISTRATION_CANCELLED",
                "Registration cancelled",
                "Your registration for " + event.getTitle() + " was cancelled."
        );

        if (removedFromConfirmed && !event.getWaitlistParticipantIds().isEmpty()) {
            Iterator<String> iterator = event.getWaitlistParticipantIds().iterator();
            String promotedParticipantId = iterator.next();
            iterator.remove();
            event.getParticipantIds().add(promotedParticipantId);
            sendNotification(
                    promotedParticipantId,
                    event,
                    "WAITLIST_PROMOTED",
                    "Registration confirmed",
                    "A place opened for " + event.getTitle() + ". Your registration is now confirmed."
            );
        }

        event.setUpdatedAt(LocalDateTime.now());
        return EventResponse.from(eventRepository.save(event));
    }

    public EventResponse postponeEvent(String id, PostponeEventRequest request) {
        Event event = getEventEntity(id);
        ensureEditable(event);
        validateSchedule(request.startAt(), request.endAt());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setStatus(EventStatus.POSTPONED);
        event.setUpdatedAt(LocalDateTime.now());
        Event savedEvent = eventRepository.save(event);
        notifyParticipants(
                savedEvent,
                "EVENT_POSTPONED",
                "Event postponed",
                savedEvent.getTitle() + " has new dates."
        );
        return EventResponse.from(savedEvent);
    }

    public EventResponse cancelEvent(String id, CancelEventRequest request) {
        Event event = getEventEntity(id);
        if (event.getStatus() == EventStatus.COMPLETED
                || event.getStatus() == EventStatus.CANCELLED) {
            throw conflict("Completed or cancelled events cannot be cancelled");
        }
        event.setStatus(EventStatus.CANCELLED);
        event.setPublished(false);
        event.setCancellationReason(request.reason().trim());
        event.setUpdatedAt(LocalDateTime.now());
        Event savedEvent = eventRepository.save(event);
        notifyParticipants(
                savedEvent,
                "EVENT_CANCELLED",
                "Event cancelled",
                savedEvent.getTitle() + " was cancelled: " + savedEvent.getCancellationReason()
        );
        return EventResponse.from(savedEvent);
    }

    public EventResponse changeStatus(String id, EventStatus newStatus) {
        Event event = getEventEntity(id);
        EventStatus currentStatus = event.getStatus();
        boolean allowed = (newStatus == EventStatus.ONGOING
                && (currentStatus == EventStatus.SCHEDULED || currentStatus == EventStatus.POSTPONED))
                || (newStatus == EventStatus.COMPLETED && currentStatus == EventStatus.ONGOING);
        if (!allowed) {
            throw conflict("Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        event.setStatus(newStatus);
        event.setUpdatedAt(LocalDateTime.now());
        Event savedEvent = eventRepository.save(event);
        notifyParticipants(
                savedEvent,
                newStatus == EventStatus.ONGOING ? "EVENT_STARTED" : "EVENT_COMPLETED",
                newStatus == EventStatus.ONGOING ? "Event started" : "Event completed",
                savedEvent.getTitle() + " is now " + newStatus.name().toLowerCase() + "."
        );
        return EventResponse.from(savedEvent);
    }

    public EventAvailabilityResponse getAvailability(String id) {
        Event event = getEventEntity(id);
        return new EventAvailabilityResponse(
                event.getId(),
                event.getCapacity(),
                event.getParticipantIds().size(),
                event.getAvailableSeats(),
                event.getWaitlistParticipantIds().size(),
                event.getWaitlistCapacity(),
                event.isFullyBooked(),
                event.getOccupancyRate()
        );
    }

    private Event getEventEntity(String id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found with id: " + id
                ));
    }

    private void applyRequest(Event event, EventRequest request) {
        event.setTitle(request.title().trim());
        event.setDescription(request.description().trim());
        event.setCategory(request.category());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setLocation(request.location().trim());
        event.setOrganizerId(request.organizerId().trim());
        event.setCapacity(request.capacity());
        event.setWaitlistCapacity(request.waitlistCapacity());
        event.setPrice(request.price());
        event.setPublished(request.published());
    }

    private void validateSchedule(LocalDateTime startAt, LocalDateTime endAt) {
        if (!endAt.isAfter(startAt)) {
            throw badRequest("Event end date must be after its start date");
        }
    }

    private void ensureEditable(Event event) {
        if (event.getStatus() == EventStatus.ONGOING
                || event.getStatus() == EventStatus.COMPLETED
                || event.getStatus() == EventStatus.CANCELLED) {
            throw conflict("Ongoing, completed, or cancelled events cannot be edited");
        }
    }

    private void ensureRegistrationOpen(Event event) {
        if (!event.isPublished()) {
            throw conflict("Registration is closed for unpublished events");
        }
        if (event.getStatus() != EventStatus.SCHEDULED
                && event.getStatus() != EventStatus.POSTPONED) {
            throw conflict("Registration is closed for events with status " + event.getStatus());
        }
        if (event.getStartAt() == null || !event.getStartAt().isAfter(LocalDateTime.now())) {
            throw conflict("Registration is closed after the event start time");
        }
    }

    private void ensureNoRegistrations(Event event, String message) {
        if (!event.getParticipantIds().isEmpty()
                || !event.getWaitlistParticipantIds().isEmpty()) {
            throw conflict(message);
        }
    }

    private void notifyParticipants(
            Event event,
            String type,
            String title,
            String message
    ) {
        event.getParticipantIds().forEach(
                participantId -> sendNotification(participantId, event, type, title, message)
        );
        event.getWaitlistParticipantIds().forEach(
                participantId -> sendNotification(participantId, event, type, title, message)
        );
    }

    private void sendNotification(
            String recipientId,
            Event event,
            String type,
            String title,
            String message
    ) {
        if (recipientId == null || recipientId.isBlank()) {
            return;
        }
        try {
            notificationClient.createNotification(new NotificationClient.NotificationRequest(
                    recipientId,
                    event.getId(),
                    type,
                    title,
                    message,
                    "/events/" + event.getId()
            ));
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Could not create {} notification for recipient {} and event {}",
                    type,
                    recipientId,
                    event.getId(),
                    exception
            );
        }
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase().contains(search.trim().toLowerCase());
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}

