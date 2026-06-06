package com.awd2026.eventservice.repository;

import com.awd2026.eventservice.entity.Event;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EventRepository extends MongoRepository<Event, String> {
}
