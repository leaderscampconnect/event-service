package com.awd2026.eventservice.controller;

import com.awd2026.eventservice.client.NotificationClient;
import com.awd2026.eventservice.entity.Event;
import com.awd2026.eventservice.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private NotificationClient notificationClient;

    private EventController eventController;

    @BeforeEach
    void setUp() {
        eventController = new EventController(eventRepository, notificationClient);
    }

    @Test
    void createsEvent() {
        Event event = event(null, "Camp opening", "Tunis");
        Event savedEvent = event("665f6b7d8c9e0f1234567890", "Camp opening", "Tunis");
        when(eventRepository.save(event)).thenReturn(savedEvent);

        Event response = eventController.createEvent(event);

        assertSame(savedEvent, response);
    }

    @Test
    void returnsEventById() {
        Event event = event("665f6b7d8c9e0f1234567890", "Camp opening", "Tunis");
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        ResponseEntity<Event> response = eventController.getEventById(event.getId());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(event, response.getBody());
    }

    @Test
    void updatesExistingEventWithoutReplacingItsId() {
        String id = "665f6b7d8c9e0f1234567890";
        Event existingEvent = event(id, "Old title", "Tunis");
        Event update = event(null, "New title", "Ariana");
        when(eventRepository.findById(id)).thenReturn(Optional.of(existingEvent));
        when(eventRepository.save(existingEvent)).thenReturn(existingEvent);

        ResponseEntity<Event> response = eventController.updateEvent(id, update);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().getId());
        assertEquals("New title", response.getBody().getTitle());
        assertEquals("Ariana", response.getBody().getLocation());
    }

    @Test
    void returnsNotFoundWhenDeletingUnknownEvent() {
        String id = "665f6b7d8c9e0f1234567890";
        when(eventRepository.existsById(id)).thenReturn(false);

        ResponseEntity<Void> response = eventController.deleteEvent(id);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(eventRepository, never()).deleteById(id);
    }

    @Test
    void combinesEventsWithNotifications() {
        List<Event> events = List.of(event("665f6b7d8c9e0f1234567890", "Camp opening", "Tunis"));
        List<String> notifications = List.of("Event created");
        when(eventRepository.findAll()).thenReturn(events);
        when(notificationClient.getNotifications()).thenReturn(notifications);

        EventController.EventWithNotificationResponse response =
                eventController.getEventsWithNotification();

        assertEquals(events, response.events());
        assertEquals(notifications, response.notifications());
    }

    private Event event(String id, String title, String location) {
        return new Event(id, title, LocalDate.of(2026, 6, 10), location);
    }
}
