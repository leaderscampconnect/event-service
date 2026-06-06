package com.awd2026.eventservice.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Document(collection = "events")
public class Event {

    @Id
    private String id;

    private String title;
    private String description;

    @Indexed
    private EventCategory category;

    @Indexed
    private EventStatus status;

    @Indexed
    private LocalDateTime startAt;

    private LocalDateTime endAt;
    private String location;
    private String organizerId;
    private int capacity;
    private int waitlistCapacity;
    private BigDecimal price;
    private boolean published;
    private Set<String> participantIds = new LinkedHashSet<>();
    private Set<String> waitlistParticipantIds = new LinkedHashSet<>();
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Event() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getWaitlistCapacity() {
        return waitlistCapacity;
    }

    public void setWaitlistCapacity(int waitlistCapacity) {
        this.waitlistCapacity = waitlistCapacity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public Set<String> getParticipantIds() {
        if (participantIds == null) {
            participantIds = new LinkedHashSet<>();
        }
        return participantIds;
    }

    public void setParticipantIds(Set<String> participantIds) {
        this.participantIds = participantIds;
    }

    public Set<String> getWaitlistParticipantIds() {
        if (waitlistParticipantIds == null) {
            waitlistParticipantIds = new LinkedHashSet<>();
        }
        return waitlistParticipantIds;
    }

    public void setWaitlistParticipantIds(Set<String> waitlistParticipantIds) {
        this.waitlistParticipantIds = waitlistParticipantIds;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getAvailableSeats() {
        return Math.max(0, capacity - getParticipantIds().size());
    }

    public boolean isFullyBooked() {
        return getAvailableSeats() == 0;
    }

    public double getOccupancyRate() {
        return capacity <= 0
                ? 0
                : Math.min(1D, (double) getParticipantIds().size() / capacity);
    }
}
