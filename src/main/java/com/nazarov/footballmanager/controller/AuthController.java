package com.nazarov.footballmanager.controller;

import com.nazarov.footballmanager.domain.User;
import com.nazarov.footballmanager.dto.user.JwtAuthenticationResponseDto;
import com.nazarov.footballmanager.dto.user.LoginRequestDto;
import com.nazarov.footballmanager.dto.user.UserRegistrationDto;
import com.nazarov.footballmanager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user registration and login")
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "Register a new user")
  @ApiResponse(responseCode = "201", description = "User registered successfully")
  @ApiResponse(responseCode = "400", description = "Invalid input or email already exists")
  @PostMapping("/register")
  public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
    User user = authService.registerUser(registrationDto);
    return ResponseEntity.created(URI.create("/api/users/" + user.getUserId()))
        .body("User registered successfully");
  }

  @Operation(summary = "Authenticate user and return JWT token")
  @ApiResponse(responseCode = "200", description = "Login successful, JWT token returned")
  @ApiResponse(responseCode = "401", description = "Invalid credentials")
  @PostMapping("/login")
  public ResponseEntity<JwtAuthenticationResponseDto> authenticateUser(
      @Valid @RequestBody LoginRequestDto loginRequestDto) {
    JwtAuthenticationResponseDto jwtResponse = authService.loginUser(loginRequestDto);
    return ResponseEntity.ok(jwtResponse);
  }
}