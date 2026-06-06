package com.awd2026.eventservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @GetMapping("/notifications")
    List<String> getNotifications();
}

