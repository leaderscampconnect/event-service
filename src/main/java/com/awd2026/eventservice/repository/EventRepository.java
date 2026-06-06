package com.awd2026.eventservice.repository;

import com.awd2026.eventservice.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}

