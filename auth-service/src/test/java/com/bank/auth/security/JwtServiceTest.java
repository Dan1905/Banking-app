package com.bank.auth.security;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.bank.auth.service.JwtService;

import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails validUser;
    private String testSecret;
    private long testExpirationMs;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();
        
        // Set test secret key via reflection (min 256 bits for HS256)
        testSecret = "test-secret-key-that-is-long-enough-for-jwt-256-bit-minimum-requirement";
        Field secretField = JwtService.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtService, testSecret);

        // Set test expiration (1 hour = 3600000 ms)
        testExpirationMs = 3600000L;
        Field expirationField = JwtService.class.getDeclaredField("expirationMs");
        expirationField.setAccessible(true);
        expirationField.set(jwtService, testExpirationMs);

        // Create test user
        validUser = new User(
                "testuser@example.com",
                "password",
                true,
                true,
                true,
                true,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }

    @Test
    @DisplayName("Should generate a valid JWT token")
    void testGenerateToken() {
        // Act
        String token = jwtService.generateToken(validUser);

        // Assert
        assertNotNull(token, "Token should not be null");
        assertFalse(token.isEmpty(), "Token should not be empty");
        assertTrue(token.contains("."), "Token should have JWT format (three parts separated by dots)");
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT token should have 3 parts");
    }

    @Test
    @DisplayName("Should generate token with custom claims")
    void testGenerateTokenWithExtraClaims() {
        // Arrange
        Map<String, Object> extraClaims = Map.of(
                "customClaim1", "value1",
                "customClaim2", 12345
        );

        // Act
        String token = jwtService.generateToken(validUser, extraClaims);

        // Assert
        assertNotNull(token, "Token should not be null");
        assertTrue(token.contains("."), "Token should have JWT format");
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void testExtractUsername() {
        // Arrange
        String token = jwtService.generateToken(validUser);

        // Act
        String extractedUsername = jwtService.extractUsername(token);

        // Assert
        assertEquals("testuser@example.com", extractedUsername, "Extracted username should match");
    }

    @Test
    @DisplayName("Should validate a valid token")
    void testIsTokenValidWithValidToken() {
        // Arrange
        String token = jwtService.generateToken(validUser);

        // Act
        boolean isValid = jwtService.isTokenValid(token, validUser);

        // Assert
        assertTrue(isValid, "Valid token should pass validation");
    }

    @Test
    @DisplayName("Should reject token with different user")
    void testIsTokenValidWithDifferentUser() {
        // Arrange
        String token = jwtService.generateToken(validUser);
        
        UserDetails differentUser = new User(
                "differentuser@example.com",
                "password",
                true,
                true,
                true,
                true,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        // Act
        boolean isValid = jwtService.isTokenValid(token, differentUser);

        // Assert
        assertFalse(isValid, "Token should be invalid for different user");
    }

    @Test
    @DisplayName("Should throw exception for invalid token signature")
    void testIsTokenValidWithInvalidSignature() {
        // Arrange
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI" +
                "xMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
                "invalid-signature";

        // Act & Assert
        JwtException ex1 = assertThrows(JwtException.class, () -> jwtService.isTokenValid(invalidToken, validUser),
            "Should throw exception for invalid signature");
        assertNotNull(ex1);
    }

    @Test
    @DisplayName("Should return correct expiration in seconds")
    void testGetExpirationSeconds() {
        // Act
        long expirationSeconds = jwtService.getExpirationSeconds();

        // Assert
        assertEquals(3600L, expirationSeconds, "Expiration should be in seconds (ms / 1000)");
    }

    @Test
    @DisplayName("Should generate different tokens for multiple calls")
    void testGenerateTokenUniqueness() throws InterruptedException {
        // Act
        String token1 = jwtService.generateToken(validUser);
        Thread.sleep(1100); // Wait 1.1 seconds to ensure different issuedAt timestamp
        String token2 = jwtService.generateToken(validUser);

        // Assert
        assertNotEquals(token1, token2, "Multiple token generations should produce different tokens (due to issuedAt)");
    }

    @Test
    @DisplayName("Should include role claim in token")
    void testTokenIncludesRoleClaim() {
        // Arrange
        UserDetails adminUser = new User(
                "admin@example.com",
                "password",
                true,
                true,
                true,
                true,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // Act
        String token = jwtService.generateToken(adminUser);
        String extractedUsername = jwtService.extractUsername(token);

        // Assert
        assertEquals("admin@example.com", extractedUsername, "Token should contain admin username");
    }

    @Test
    @DisplayName("Should handle user with multiple authorities")
    void testTokenWithMultipleAuthorities() {
        // Arrange
        UserDetails multiAuthUser = new User(
                "user@example.com",
                "password",
                true,
                true,
                true,
                true,
                java.util.List.of(
                        new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                        new SimpleGrantedAuthority("ROLE_EMPLOYEE")
                )
        );

        // Act
        String token = jwtService.generateToken(multiAuthUser);
        String extractedUsername = jwtService.extractUsername(token);

        // Assert
        assertEquals("user@example.com", extractedUsername, "Should extract correct username");
        assertNotNull(token, "Token should be generated successfully");
    }

    @Test
    @DisplayName("Should throw exception for null token")
    void testExtractUsernameWithNullToken() {
        // Act & Assert
        Exception ex2 = assertThrows(Exception.class, () -> jwtService.extractUsername(null), "Should throw exception for null token");
        assertNotNull(ex2);
    }

    @Test
    @DisplayName("Should throw exception for empty token")
    void testExtractUsernameWithEmptyToken() {
        // Act & Assert
        Exception ex3 = assertThrows(Exception.class, () -> jwtService.extractUsername(""), "Should throw exception for empty token");
        assertNotNull(ex3);
    }

    @Test
    @DisplayName("Should throw exception for malformed token")
    void testExtractUsernameWithMalformedToken() {
        // Act & Assert
        Exception ex4 = assertThrows(Exception.class, () -> jwtService.extractUsername("this-is-not-a-valid-jwt-token"),
            "Should throw exception for malformed token");
        assertNotNull(ex4);
    }

    @Test
    @DisplayName("Should validate token with special characters in username")
    void testTokenWithSpecialCharactersInUsername() {
        // Arrange
        UserDetails specialUser = new User(
                "user+special@domain.co.uk",
                "password",
                true,
                true,
                true,
                true,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        // Act
        String token = jwtService.generateToken(specialUser);
        String extractedUsername = jwtService.extractUsername(token);
        boolean isValid = jwtService.isTokenValid(token, specialUser);

        // Assert
        assertEquals("user+special@domain.co.uk", extractedUsername, "Should handle special characters");
        assertTrue(isValid, "Token should be valid for user with special characters");
    }

    @Test
    @DisplayName("Should maintain consistency between generated and validated tokens")
    void testTokenConsistency() {
        // Arrange & Act
        String token = jwtService.generateToken(validUser);
        String username1 = jwtService.extractUsername(token);
        boolean isValid = jwtService.isTokenValid(token, validUser);
        String username2 = jwtService.extractUsername(token);

        // Assert
        assertEquals(username1, username2, "Username extraction should be consistent");
        assertTrue(isValid, "Token should remain valid across multiple validations");
    }

    @Test
    @DisplayName("Should generate short-lived token with custom expiration")
    void testShortLivedToken() throws Exception {
        // Arrange - Set short expiration (10 seconds)
        Field expirationField = JwtService.class.getDeclaredField("expirationMs");
        expirationField.setAccessible(true);
        expirationField.set(jwtService, 10000L);

        // Act
        long shortExpirationSeconds = jwtService.getExpirationSeconds();

        // Assert
        assertEquals(10L, shortExpirationSeconds, "Expiration should reflect custom value");
    }
}

