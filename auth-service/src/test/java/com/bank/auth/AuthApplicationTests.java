package com.bank.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bank.auth.controller.AuthController;
import com.bank.auth.repository.UserRepository;
import com.bank.auth.service.AuthService;
import com.bank.auth.service.JwtService;

@SpringBootTest
@DisplayName("Auth Application Integration Tests")
class AuthApplicationTests {

	@Autowired(required = false)
	private AuthController authController;

	@Autowired(required = false)
	private AuthService authService;

	@Autowired(required = false)
	private JwtService jwtService;

	@Autowired(required = false)
	private UserRepository userRepository;

	@Autowired(required = false)
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("Should load application context successfully")
	void contextLoads() {
		// Assert
		assertNotNull(authController, "AuthController should be loaded");
	}

	@Test
	@DisplayName("AuthService component should be available")
	void authServiceShouldBeLoaded() {
		assertNotNull(authService, "AuthService should be available in context");
	}

	@Test
	@DisplayName("JwtService component should be available")
	void jwtServiceShouldBeLoaded() {
		assertNotNull(jwtService, "JwtService should be available in context");
	}

	@Test
	@DisplayName("UserRepository component should be available")
	void userRepositoryShouldBeLoaded() {
		assertNotNull(userRepository, "UserRepository should be available in context");
	}

	@Test
	@DisplayName("PasswordEncoder bean should be available")
	void passwordEncoderShouldBeLoaded() {
		assertNotNull(passwordEncoder, "PasswordEncoder should be available in context");
	}

	@Test
	@DisplayName("AuthController should be properly wired")
	void authControllerShouldBeProperlyWired() {
		assertNotNull(authController, "AuthController should not be null");
		assertNotNull(authService, "AuthService should be injected into AuthController");
	}

	@Test
	@DisplayName("All authentication components should be initialized")
	void allComponentsShouldBeInitialized() {
		assertNotNull(authController, "Controller not initialized");
		assertNotNull(authService, "Service not initialized");
		assertNotNull(jwtService, "JWT Service not initialized");
		assertNotNull(userRepository, "Repository not initialized");
		assertNotNull(passwordEncoder, "Password encoder not initialized");
	}
}

