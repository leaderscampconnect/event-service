package com.awd2026.eventservice.controller;

import com.awd2026.eventservice.client.NotificationClient;
import com.awd2026.eventservice.exception.GlobalExceptionHandler;
import com.awd2026.eventservice.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import(GlobalExceptionHandler.class)
class EventControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private NotificationClient notificationClient;

    @Test
    void returnsStructuredValidationErrors() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "short",
                                  "category": "ADVENTURE",
                                  "startAt": "2026-07-10T09:00:00",
                                  "endAt": "2026-07-12T18:00:00",
                                  "location": "",
                                  "organizerId": "",
                                  "capacity": 0,
                                  "waitlistCapacity": -1,
                                  "price": -5,
                                  "published": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.title").exists())
                .andExpect(jsonPath("$.validationErrors.capacity").exists())
                .andExpect(jsonPath("$.path").value("/events"));
    }
}
