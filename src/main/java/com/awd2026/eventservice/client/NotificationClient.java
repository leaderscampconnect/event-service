package com.awd2026.eventservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(
        name = "notification-service",
        url = "${notification.service.url:${NOTIFICATION_SERVICE_URL:}}"
)
public interface NotificationClient {

    @GetMapping("/notifications")
    List<NotificationResponse> getNotifications();

    @PostMapping("/notifications")
    NotificationResponse createNotification(@RequestBody NotificationRequest request);

    record NotificationRequest(
            String recipientId,
            String eventId,
            String type,
            String title,
            String message,
            String actionUrl
    ) {
    }

    record NotificationResponse(
            String id,
            String recipientId,
            String eventId,
            String type,
            String title,
            String message,
            boolean read,
            LocalDateTime createdAt,
            String actionUrl
    ) {
    }
}
