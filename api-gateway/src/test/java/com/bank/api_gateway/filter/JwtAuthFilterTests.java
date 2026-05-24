package com.bank.api_gateway.filter;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.function.ServerRequest;

import com.bank.api_gateway.exception.GatewayAuthException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtAuthFilterTests {

    private JwtAuthFilter jwtAuthFilter;
    private ServerRequest request;
    private ServerRequest.Headers headers;

    private void prepare() {
        jwtAuthFilter = new JwtAuthFilter();
        ReflectionTestUtils.setField(jwtAuthFilter, "secret", "test-secret-key-that-is-long-enough-for-hs256");

        request = mock(ServerRequest.class);
        headers = mock(ServerRequest.Headers.class);
        when(request.headers()).thenReturn(headers);
    }

    @Test
    @DisplayName("public auth paths should bypass JWT validation")
    void testPublicPathBypassesValidation() {
        prepare();
        when(request.path()).thenReturn("/api/auth/login");

        assertDoesNotThrow(() -> jwtAuthFilter.filter(request));
    }

    @Test
    @DisplayName("internal paths should be forbidden")
    void testInternalPathForbidden() {
        prepare();
        when(request.path()).thenReturn("/api/accounts/internal/refresh");

        GatewayAuthException ex = assertThrows(GatewayAuthException.class, () -> jwtAuthFilter.filter(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("missing Authorization header should be unauthorized")
    void testMissingAuthorizationHeader() {
        prepare();
        when(request.path()).thenReturn("/api/accounts/123");
        when(headers.firstHeader("Authorization")).thenReturn(null);

        GatewayAuthException ex = assertThrows(GatewayAuthException.class, () -> jwtAuthFilter.filter(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("valid Bearer token should pass validation")
    void testValidTokenPasses() {
        prepare();
        when(request.path()).thenReturn("/api/accounts/123");
        String token = Jwts.builder()
                .subject("john@example.com")
                .signWith(Keys.hmacShaKeyFor("test-secret-key-that-is-long-enough-for-hs256".getBytes(StandardCharsets.UTF_8)))
                .compact();
        when(headers.firstHeader("Authorization")).thenReturn("Bearer " + token);

        assertDoesNotThrow(() -> jwtAuthFilter.filter(request));
    }

    @Test
    @DisplayName("invalid token should be unauthorized")
    void testInvalidTokenRejected() {
        prepare();
        when(request.path()).thenReturn("/api/accounts/123");
        when(headers.firstHeader("Authorization")).thenReturn("Bearer invalid.token.value");

        GatewayAuthException ex = assertThrows(GatewayAuthException.class, () -> jwtAuthFilter.filter(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }
}
