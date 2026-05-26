package com.bank.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.bank.transaction.dto.UserEmailResponse;

@FeignClient(name = "auth-service", url = "${auth.service.url}")
public interface AuthClient {

    @GetMapping("/api/auth/internal/users/{userId}")
    UserEmailResponse getUserEmail(
            @RequestHeader("X-Internal-Token") String internalToken,
            @PathVariable("userId") Long userId);
}
