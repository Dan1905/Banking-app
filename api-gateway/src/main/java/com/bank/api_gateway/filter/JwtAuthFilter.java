package com.bank.api_gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import com.bank.api_gateway.exception.GatewayAuthException;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;


@Component
public class JwtAuthFilter{
    
    @Value("${jwt.secret}")
    private String secret;

    //paths to skip jwt check 
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/health",
        "/actuator/health"
    );

    public void filter(ServerRequest request) {
        String path = request.path();

        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return;
        }

        if (path.contains("/internal/")) {
            throw new GatewayAuthException(HttpStatus.FORBIDDEN, "Access denied");
        }

        String authHeader = request.headers().firstHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new GatewayAuthException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        try {
            Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(authHeader.substring(7));
        } catch (JwtException e) {
            throw new GatewayAuthException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }
    
}
