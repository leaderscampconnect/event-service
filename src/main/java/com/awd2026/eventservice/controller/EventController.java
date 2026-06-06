package com.awd2026.eventservice.controller;

import com.awd2026.eventservice.client.NotificationClient;
import com.awd2026.eventservice.entity.Event;
import com.awd2026.eventservice.repository.EventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventRepository eventRepository;
    private final NotificationClient notificationClient;

    public EventController(EventRepository eventRepository, NotificationClient notificationClient) {
        this.eventRepository = eventRepository;
        this.notificationClient = notificationClient;
    }

    @GetMapping
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @GetMapping("/with-notification")
    public EventWithNotificationResponse getEventsWithNotification() {
        return new EventWithNotificationResponse(
                eventRepository.findAll(),
                notificationClient.getNotifications()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable String id) {
        return eventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Event createEvent(@RequestBody Event event) {
        return eventRepository.save(event);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable String id, @RequestBody Event event) {
        return eventRepository.findById(id)
                .map(existingEvent -> {
                    existingEvent.setTitle(event.getTitle());
                    existingEvent.setDate(event.getDate());
                    existingEvent.setLocation(event.getLocation());
                    return ResponseEntity.ok(eventRepository.save(existingEvent));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String id) {
        if (!eventRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        eventRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notifications")
    public List<String> getNotificationsFromNotificationService() {
        return notificationClient.getNotifications();
    }

    public record EventWithNotificationResponse(List<Event> events, List<String> notifications) {
    }
}
