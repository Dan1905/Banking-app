package com.bank.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bank.auth.dto.AdminCreateUserRequest;
import com.bank.auth.dto.AuthResponse;
import com.bank.auth.dto.LoginRequest;
import com.bank.auth.dto.RegisterRequest;
import com.bank.auth.entity.Role;
import com.bank.auth.entity.User;
import com.bank.auth.exception.EmailAlreadyExistsException;
import com.bank.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private AdminCreateUserRequest adminCreateUserRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole(Role.CUSTOMER);

        adminCreateUserRequest = new AdminCreateUserRequest();
        adminCreateUserRequest.setFullName("Jane Employee");
        adminCreateUserRequest.setEmail("jane@example.com");
        adminCreateUserRequest.setPassword("password123");
        adminCreateUserRequest.setRole(Role.EMPLOYEE);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");

        testUser = new User();
        testUser.setEmail("john@example.com");
        testUser.setFullName("John Doe");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.CUSTOMER);
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void testRegisterSuccess() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response, "AuthResponse should not be null");
        assertEquals("john@example.com", response.getEmail(), "Email should match");
        assertEquals("John Doe", response.getFullName(), "Full name should match");
        assertEquals(Role.CUSTOMER, response.getRole(), "Role should match");
        assertEquals("jwt-token", response.getToken(), "Token should match");
        assertEquals(3600L, response.getExpiresIn(), "Expiration should match");

        // Verify interactions
        verify(userRepository, times(1)).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder, times(1)).encode(registerRequest.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtService, times(1)).generateToken(any(User.class));
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email is already registered")
    void testRegisterWithExistingEmail() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        EmailAlreadyExistsException ex1 = assertThrows(EmailAlreadyExistsException.class, () -> authService.register(registerRequest),
            "Should throw EmailAlreadyExistsException");
        assertNotNull(ex1);

        verify(userRepository, times(1)).existsByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should encode password during registration")
    void testPasswordEncodingOnRegister() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        // Act
        authService.register(registerRequest);

        // Assert
        verify(passwordEncoder, times(1)).encode(registerRequest.getPassword());
    }

    @Test
    @DisplayName("Should override role to CUSTOMER during registration")
    void testRegisterWithCustomRole() {
        // Arrange
        registerRequest.setRole(Role.EMPLOYEE);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");

        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.CUSTOMER, userCaptor.getValue().getRole(), "Role should be CUSTOMER");
        assertEquals(Role.CUSTOMER, response.getRole(), "Role should be CUSTOMER");
    }

    @Test
    @DisplayName("Should allow admin to create a user with requested role")
    void testAdminCreateUserSuccess() {
        // Arrange
        when(userRepository.existsByEmail(adminCreateUserRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(adminCreateUserRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        // Act
        AuthResponse response = authService.createUserAsAdmin(adminCreateUserRequest);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.EMPLOYEE, userCaptor.getValue().getRole(), "Role should be preserved");
        assertEquals(Role.EMPLOYEE, response.getRole(), "Response role should match");
    }

    @Test
    @DisplayName("Admin create should reject duplicate email")
    void testAdminCreateUserDuplicateEmail() {
        // Arrange
        when(userRepository.existsByEmail(adminCreateUserRequest.getEmail())).thenReturn(true);

        // Act & Assert
        EmailAlreadyExistsException ex2 = assertThrows(EmailAlreadyExistsException.class, () -> authService.createUserAsAdmin(adminCreateUserRequest),
            "Should throw EmailAlreadyExistsException");
        assertNotNull(ex2);

        verify(userRepository, times(1)).existsByEmail(adminCreateUserRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLoginSuccess() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(java.util.Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response, "AuthResponse should not be null");
        assertEquals("john@example.com", response.getEmail(), "Email should match");
        assertEquals("jwt-token", response.getToken(), "Token should match");

        verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateToken(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found during login")
    void testLoginWithNonExistentUser() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(java.util.Optional.empty());

        // Act & Assert
        BadCredentialsException ex3 = assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest),
            "Should throw exception for non-existent user");
        assertNotNull(ex3);

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should throw exception when password is incorrect")
    void testLoginWithWrongPassword() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        BadCredentialsException ex4 = assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest),
            "Should throw exception for wrong password");
        assertNotNull(ex4);

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should generate unique tokens for multiple registrations")
    void testMultipleRegistrationsGenerateUniqueTokens() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class)))
                .thenReturn("token1")
                .thenReturn("token2");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        // Act
        AuthResponse response1 = authService.register(registerRequest);
        AuthResponse response2 = authService.register(registerRequest);

        // Assert
        assertEquals("token1", response1.getToken());
        assertEquals("token2", response2.getToken());
    }

    @Test
    @DisplayName("Should include role in JWT token data")
    void testRoleIncludedInToken() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(java.util.Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-with-role");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        // Act
        authService.login(loginRequest);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(jwtService).generateToken(userCaptor.capture());
        assertEquals(Role.CUSTOMER, userCaptor.getValue().getRole());
    }

    @Test
    @DisplayName("Should preserve email in AuthResponse")
    void testEmailPreservedInResponse() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertEquals(registerRequest.getEmail(), response.getEmail());
    }
}
