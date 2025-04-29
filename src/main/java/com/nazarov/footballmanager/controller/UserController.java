package com.nazarov.footballmanager.controller;

import com.nazarov.footballmanager.dto.user.UserViewDto;
import com.nazarov.footballmanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "APIs related to user profiles")
public class UserController {

  private final UserService userService;

  @Operation(summary = "Get current user's profile",
      security = @SecurityRequirement(name = "bearerAuth")) // Link to security scheme in OpenAPI config
  @ApiResponse(responseCode = "200", description = "User profile retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Unauthorized - user not logged in")
  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()") // Ensure user is logged in
  public ResponseEntity<UserViewDto> getCurrentUser() {
    UserViewDto userProfile = userService.getCurrentUserProfile();
    return ResponseEntity.ok(userProfile);
  }

}