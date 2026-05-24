package com.bank.auth.dto;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bank.auth.entity.Role;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("RegisterRequest Validation Tests")
class RegisterRequestValidationTest {

    private Validator validator;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        registerRequest = new RegisterRequest();
        registerRequest.setFullName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole(Role.CUSTOMER);
    }

    @Test
    @DisplayName("Should validate a valid RegisterRequest")
    void testValidRegisterRequest() {
        // Act
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        // Assert
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    @DisplayName("Should reject RegisterRequest with null fullName")
    void testRegisterRequestWithNullFullName() {
        // Arrange
        registerRequest.setFullName(null);

        // Act
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Full name is required")),
                "Should contain full name required message");
    }

    @Test
    @DisplayName("Should reject RegisterRequest with blank fullName")
    void testRegisterRequestWithBlankFullName() {
        // Arrange
        registerRequest.setFullName("   ");

        // Act
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation");
    }

    @Test
    @DisplayName("Should reject RegisterRequest with empty fullName")
    void testRegisterRequestWithEmptyFullName() {
        // Arrange
        registerRequest.setFullName("");

        // Act
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation");
    }

    @Test
    @DisplayName("Should reject RegisterRequest with null email")
    void testRegisterRequestWithNullEmail() {
        // Arrange
        registerRequest.setEmail(null);

        // Act
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Email is required")),
                "Should contain email required message");
    }

    @Test
    @DisplayName("Should reject RegisterRequest with blank email")
    void testRegisterRequestWithBlankEmail() {
        // Arrange
        registerRequest.setEmail("   ");

        // Act
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        // Assert
        assertTrue(violations.size() >= 1, "Should have at least one violation for blank email");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Email is required") || 
                               v.getMessage().contains("must be a well-formed email")),
                "Should contain email validation error");
    }

    @Test
    @DisplayName("Should reject RegisterRequest with invalid email format")
    void testRegisterRequestWithInvalidEmailFormat() {
        // Arrange
        registerRequest.setEmail("invalid-email");

        // Act
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Email must be valid")),
                "Should contain invalid email message");
    }

    @Test
    @DisplayName("Should reject RegisterRequest with email missing domain")
    void testRegisterRequestWithEmailMissingDomain() {
        // Arrange
        registerRequest.setEmail("john@");

        // Act
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation");
    }

    @Test
    @DisplayName("Should reject RegisterRequest with email missing local part")
    void testRegisterRequestWithEmailMissingLocalPart() {
        // Arrange
        registerRequest.setEmail("@example.com");

        // Act
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation");
    }

    @Test
    @DisplayName("Should accept valid email formats")
    void testRegisterRequestWithValidEmailFormats() {
        // Test various valid email formats
        String[] validEmails = {
                "user@example.com",
                "user.name@example.com",
                "user+tag@example.co.uk",
                "user_123@example-domain.com"
        };

        for (String email : validEmails) {
            registerRequest.setEmail(email);
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);
            assertEquals(0, violations.size(), "Email " + email + " should be valid");
        }
    }

    @Test
    @DisplayName("Should reject RegisterRequest with null password")
    void testRegisterRequestWithNullPassword() {
        // Arrange
        registerRequest.setPassword(null);

        // Act
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Password is required")),
                "Should contain password required message");
    }

    @Test
    @DisplayName("Should reject RegisterRequest with blank password")
    void testRegisterRequestWithBlankPassword() {
        // Arrange
        registerRequest.setPassword("   ");

        // Act
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        // Assert
        assertTrue(violations.size() >= 1, "Should have at least one violation for blank password");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Password is required") || 
                               v.getMessage().contains("must be at least 8 characters")),
                "Should contain password validation error");
    }

    @Test
    @DisplayName("Should reject RegisterRequest with password less than 8 characters")
    void testRegisterRequestWithShortPassword() {
        // Arrange
        registerRequest.setPassword("pass123");

        // Act
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        // Assert
        assertEquals(1, violations.size(), "Should have exactly one violation");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("at least 8 characters")),
                "Should contain length validation message");
    }

    @Test
    @DisplayName("Should accept password with exactly 8 characters")
    void testRegisterRequestWithExactly8CharPassword() {
        // Arrange
        registerRequest.setPassword("password");

        // Act
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        // Assert
        assertTrue(violations.isEmpty(), "Password with exactly 8 characters should be valid");
    }

    @Test
    @DisplayName("Should accept password with more than 8 characters")
    void testRegisterRequestWithLongPassword() {
        // Arrange
        registerRequest.setPassword("password123456789");

        // Act
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        // Assert
        assertTrue(violations.isEmpty(), "Password longer than 8 characters should be valid");
    }

    @Test
    @DisplayName("Should handle multiple validation errors simultaneously")
    void testRegisterRequestMultipleValidationErrors() {
        // Arrange
        registerRequest.setFullName(null);
        registerRequest.setEmail("invalid");
        registerRequest.setPassword("short");

        // Act
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        // Assert
        assertEquals(3, violations.size(), "Should have three violations");
    }

    @Test
    @DisplayName("Should have default role as CUSTOMER when not specified")
    void testDefaultRoleIsCustomer() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPassword("password123");
        // Role not explicitly set

        // Act & Assert
        assertEquals(Role.CUSTOMER, request.getRole(), "Default role should be CUSTOMER");
    }

    @Test
    @DisplayName("Should preserve custom roles")
    void testCustomRoles() {
        // Test EMPLOYEE role
        registerRequest.setRole(Role.EMPLOYEE);
        assertEquals(Role.EMPLOYEE, registerRequest.getRole(), "Should preserve EMPLOYEE role");

        // Test ADMIN role
        registerRequest.setRole(Role.ADMIN);
        assertEquals(Role.ADMIN, registerRequest.getRole(), "Should preserve ADMIN role");

        // Test CUSTOMER role
        registerRequest.setRole(Role.CUSTOMER);
        assertEquals(Role.CUSTOMER, registerRequest.getRole(), "Should preserve CUSTOMER role");
    }

    @Test
    @DisplayName("Should validate successfully with all three role types")
    void testValidationWithAllRoleTypes() {
        Role[] roles = { Role.CUSTOMER, Role.EMPLOYEE, Role.ADMIN };

        for (Role role : roles) {
            registerRequest.setRole(role);
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);
            assertTrue(violations.isEmpty(), "Should be valid with " + role + " role");
        }
    }
}
