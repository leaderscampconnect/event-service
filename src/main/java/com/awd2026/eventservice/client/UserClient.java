package com.awd2026.eventservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        url = "${user.service.url:${USER_SERVICE_URL:}}"
)
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserResponse getUser(@PathVariable Long id);

    record UserResponse(
            Long id,
            String firstName,
            String lastName,
            String email,
            String phone,
            String role
    ) {
    }
}
