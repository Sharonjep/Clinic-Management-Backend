package com.clinic.management.controller;

import com.clinic.management.dto.auth.JwtResponse;
import com.clinic.management.dto.auth.LoginRequest;
import com.clinic.management.dto.auth.RegisterRequest;
import com.clinic.management.dto.auth.UserResponse;
import com.clinic.management.dto.common.ApiResponse;
import com.clinic.management.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and User Registration Endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticates credentials and returns a Bearer JWT token.")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", jwtResponse));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register new staff user", description = "Admin-only endpoint to provision receptionist, doctor, or admin accounts.")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        UserResponse userResponse = authService.registerUser(registerRequest);
        return new ResponseEntity<>(ApiResponse.success("User registered successfully", userResponse), HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated profile", description = "Returns details of the currently authenticated user.")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse currentUser = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(currentUser));
    }
}
