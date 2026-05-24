package com.bank.auth.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("LoginRequest Validation Tests")
class LoginRequestValidationTest {

    private Validator validator;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    @DisplayName("Should validate a valid LoginRequest")
    void testValidLoginRequest() {
        // Act
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // Assert
        assertTrue(violations.isEmpty(), "Valid login request should have no violations");
    }

    @Test
    @DisplayName("Should reject LoginRequest with null email")
    void testLoginRequestWithNullEmail() {
        // Arrange
        loginRequest.setEmail(null);

        // Act
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Email is required")),
                "Should contain email required message");
    }

    @Test
    @DisplayName("Should reject LoginRequest with blank email")
    void testLoginRequestWithBlankEmail() {
        // Arrange
        loginRequest.setEmail("   ");

        // Act
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // Assert
        assertTrue(violations.size() >= 1, "Should have at least one violation for blank email");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Email is required") || 
                               v.getMessage().contains("must be a well-formed email")),
                "Should contain email validation error");
    }

    @Test
    @DisplayName("Should reject LoginRequest with empty email")
    void testLoginRequestWithEmptyEmail() {
        // Arrange
        loginRequest.setEmail("");

        // Act
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation");
    }

    @Test
    @DisplayName("Should reject LoginRequest with invalid email format")
    void testLoginRequestWithInvalidEmailFormat() {
        // Arrange
        loginRequest.setEmail("invalid-email");

        // Act
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Email must be valid")),
                "Should contain invalid email message");
    }

    @Test
    @DisplayName("Should reject LoginRequest with null password")
    void testLoginRequestWithNullPassword() {
        // Arrange
        loginRequest.setPassword(null);

        // Act
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Password is required")),
                "Should contain password required message");
    }

    @Test
    @DisplayName("Should reject LoginRequest with blank password")
    void testLoginRequestWithBlankPassword() {
        // Arrange
        loginRequest.setPassword("   ");

        // Act
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation");
    }

    @Test
    @DisplayName("Should reject LoginRequest with empty password")
    void testLoginRequestWithEmptyPassword() {
        // Arrange
        loginRequest.setPassword("");

        // Act
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation");
    }

    @Test
    @DisplayName("Should handle multiple validation errors simultaneously")
    void testLoginRequestMultipleValidationErrors() {
        // Arrange
        loginRequest.setEmail(null);
        loginRequest.setPassword(null);

        // Act
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // Assert
        assertEquals(2, violations.size(), "Should have two violations");
    }

    @Test
    @DisplayName("Should accept various valid email formats")
    void testLoginRequestWithValidEmailFormats() {
        String[] validEmails = {
                "user@example.com",
                "test.user@example.co.uk",
                "user+tag@domain.com",
                "user_123@example-domain.org"
        };

        for (String email : validEmails) {
            loginRequest.setEmail(email);
            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);
            assertEquals(0, violations.size(), "Email " + email + " should be valid");
        }
    }

    @Test
    @DisplayName("Should accept passwords of any length (no minimum enforced)")
    void testLoginRequestWithVariousPasswordLengths() {
        String[] passwords = {
                "a",
                "short",
                "password",
                "very_long_password_with_special_chars_!@#",
                "1234567890"
        };

        for (String password : passwords) {
            loginRequest.setPassword(password);
            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);
            assertEquals(0, violations.size(), "Password '" + password + "' should be valid");
        }
    }
}
