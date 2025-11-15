package cg.xis.eradio.controller;

import cg.xis.eradio.dto.request.LoginRequest;
import cg.xis.eradio.dto.request.RegisterRequest;
import cg.xis.eradio.dto.response.AuthResponse;
import cg.xis.eradio.dto.response.UserProfileResponse;
import cg.xis.eradio.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor // This generates the constructor for final fields
@Tag(name = "Authentication", description = "Authentication endpoints for user registration and login")
public class AuthController {

    private final UserService userService; // Must be final for @RequiredArgsConstructor

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account and returns a JWT token")
    @ApiResponse(responseCode = "201", description = "User successfully registered")
    @ApiResponse(responseCode = "400", description = "Invalid input or user already exists")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login user", description = "Authenticates a user and returns a JWT token")
    @ApiResponse(responseCode = "200", description = "User successfully authenticated")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user profile", description = "Returns the profile of the authenticated user")
    @ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile() {
        var user = userService.getCurrentUser();
        var response = new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName()
        );
        return ResponseEntity.ok(response);
    }
}