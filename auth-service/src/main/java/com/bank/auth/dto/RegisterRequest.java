package com.bank.auth.dto;

import com.bank.auth.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
	@NotBlank(message = "Full name is required")
    private String fullName;
 
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
 
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
 
    // Included for internal/admin flows; public registration always uses CUSTOMER.
    private Role role = Role.CUSTOMER;
}

