package cg.xis.eradio.controller;

import cg.xis.eradio.dto.request.LoginRequest;
import cg.xis.eradio.dto.request.RegisterRequest;
import cg.xis.eradio.dto.response.AuthResponse;
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
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints for user registration and login")
public class AuthController {

    private final UserService userService;

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
}

