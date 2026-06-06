package com.awd2026.eventservice.service;

import com.awd2026.eventservice.client.NotificationClient;
import com.awd2026.eventservice.dto.CancelEventRequest;
import com.awd2026.eventservice.dto.EventRequest;
import com.awd2026.eventservice.dto.EventResponse;
import com.awd2026.eventservice.dto.PostponeEventRequest;
import com.awd2026.eventservice.dto.RegistrationResponse;
import com.awd2026.eventservice.entity.Event;
import com.awd2026.eventservice.entity.EventCategory;
import com.awd2026.eventservice.entity.EventStatus;
import com.awd2026.eventservice.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private NotificationClient notificationClient;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, notificationClient);
        lenient().when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsPublishedScheduledEvent() {
        EventResponse response = eventService.createEvent(request(2, 1, true));

        assertEquals(EventStatus.SCHEDULED, response.status());
        assertTrue(response.published());
        assertEquals(2, response.availableSeats());
        verify(notificationClient).createNotification(any());
    }

    @Test
    void rejectsInvalidDateRange() {
        LocalDateTime startAt = LocalDateTime.now().plusDays(2);
        EventRequest request = new EventRequest(
                "Forest camp",
                "A realistic forest camping event.",
                EventCategory.ADVENTURE,
                startAt,
                startAt.minusHours(1),
                "Tunis",
                "organizer-1",
                10,
                2,
                BigDecimal.TEN,
                true
        );

        assertThrows(ResponseStatusException.class, () -> eventService.createEvent(request));
    }

    @Test
    void confirmsParticipantWhenSeatIsAvailable() {
        Event event = event(1, 1);
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(event));

        RegistrationResponse response =
                eventService.registerParticipant("event-1", "camper-1");

        assertEquals(RegistrationResponse.RegistrationStatus.CONFIRMED,
                response.registrationStatus());
        assertTrue(event.getParticipantIds().contains("camper-1"));
    }

    @Test
    void waitlistsParticipantWhenEventIsFull() {
        Event event = event(1, 1);
        event.getParticipantIds().add("camper-1");
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(event));

        RegistrationResponse response =
                eventService.registerParticipant("event-1", "camper-2");

        assertEquals(RegistrationResponse.RegistrationStatus.WAITLISTED,
                response.registrationStatus());
        assertEquals(1, response.waitlistPosition());
    }

    @Test
    void promotesFirstWaitlistedParticipantAfterCancellation() {
        Event event = event(1, 2);
        event.getParticipantIds().add("camper-1");
        event.getWaitlistParticipantIds().add("camper-2");
        event.getWaitlistParticipantIds().add("camper-3");
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(event));

        EventResponse response =
                eventService.cancelRegistration("event-1", "camper-1");

        assertTrue(response.participantIds().contains("camper-2"));
        assertFalse(response.waitlistParticipantIds().contains("camper-2"));
        assertTrue(response.waitlistParticipantIds().contains("camper-3"));
    }

    @Test
    void preventsDeletingEventWithRegistrations() {
        Event event = event(5, 2);
        event.getParticipantIds().add("camper-1");
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(event));

        assertThrows(ResponseStatusException.class,
                () -> eventService.deleteEvent("event-1"));
    }

    @Test
    void postponesEventAndKeepsRegistrations() {
        Event event = event(5, 2);
        event.getParticipantIds().add("camper-1");
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(event));
        LocalDateTime newStart = LocalDateTime.now().plusDays(10);

        EventResponse response = eventService.postponeEvent(
                "event-1",
                new PostponeEventRequest(newStart, newStart.plusHours(3))
        );

        assertEquals(EventStatus.POSTPONED, response.status());
        assertEquals(newStart, response.startAt());
        assertTrue(response.participantIds().contains("camper-1"));
    }

    @Test
    void cancelsEventWithReason() {
        Event event = event(5, 2);
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(event));

        EventResponse response = eventService.cancelEvent(
                "event-1",
                new CancelEventRequest("Unsafe weather")
        );

        assertEquals(EventStatus.CANCELLED, response.status());
        assertEquals("Unsafe weather", response.cancellationReason());
        assertFalse(response.published());
    }

    private EventRequest request(int capacity, int waitlistCapacity, boolean published) {
        LocalDateTime startAt = LocalDateTime.now().plusDays(2);
        return new EventRequest(
                "Forest camp",
                "A realistic forest camping event.",
                EventCategory.ADVENTURE,
                startAt,
                startAt.plusHours(4),
                "Tunis",
                "organizer-1",
                capacity,
                waitlistCapacity,
                new BigDecimal("45.00"),
                published
        );
    }

    private Event event(int capacity, int waitlistCapacity) {
        Event event = new Event();
        event.setId("event-1");
        event.setTitle("Forest camp");
        event.setDescription("A realistic forest camping event.");
        event.setCategory(EventCategory.ADVENTURE);
        event.setStatus(EventStatus.SCHEDULED);
        event.setStartAt(LocalDateTime.now().plusDays(2));
        event.setEndAt(LocalDateTime.now().plusDays(2).plusHours(4));
        event.setLocation("Tunis");
        event.setOrganizerId("organizer-1");
        event.setCapacity(capacity);
        event.setWaitlistCapacity(waitlistCapacity);
        event.setPrice(new BigDecimal("45.00"));
        event.setPublished(true);
        event.setParticipantIds(new LinkedHashSet<>());
        event.setWaitlistParticipantIds(new LinkedHashSet<>());
        return event;
    }
}
