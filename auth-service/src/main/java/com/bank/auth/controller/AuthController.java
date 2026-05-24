package com.bank.auth.controller;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bank.auth.dto.AdminCreateUserRequest;
import com.bank.auth.dto.AuthResponse;
import com.bank.auth.dto.LoginRequest;
import com.bank.auth.dto.RegisterRequest;
import com.bank.auth.exception.ForbiddenOperationException;
import com.bank.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
 
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }
 
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/admin/users")
    public ResponseEntity<AuthResponse> createUserAsAdmin(
            @RequestHeader(value = "X-User-Role", required = false) String callerRole,
            @Valid @RequestBody AdminCreateUserRequest request) {
        if (!isAdminRole(callerRole)) {
            throw new ForbiddenOperationException("Admin role required");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createUserAsAdmin(request));
    }
 
    // Simple liveness check (no auth required)
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("auth-service up");
    }

    private boolean isAdminRole(String roleHeader) {
        if (roleHeader == null) {
            return false;
        }
        String normalized = roleHeader.trim().toUpperCase(Locale.ROOT);
        return "ADMIN".equals(normalized) || "ROLE_ADMIN".equals(normalized);
    }
}
