package com.nazarov.footballmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nazarov.footballmanager.domain.Role;
import com.nazarov.footballmanager.domain.User;
import com.nazarov.footballmanager.dto.user.UserViewDto;
import com.nazarov.footballmanager.exception.ResourceNotFoundException;
import com.nazarov.footballmanager.repository.UserRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private SecurityContext securityContext;
  @Mock
  private Authentication authentication;
  @Mock
  private UserDetails userDetails;

  @InjectMocks
  private UserService userService;

  private User user;
  private Role userRole;

  @BeforeEach
  void setUp() {
    // Set up mock security context before each test
    SecurityContextHolder.setContext(securityContext);

    userRole = new Role(1, "ROLE_USER");
    user = User.builder()
        .userId(1)
        .name("Current User")
        .email("current@example.com")
        .password("encodedPassword")
        .dateOfBirth(LocalDate.of(1990, 1, 1))
        .playingPosition("Defender")
        .roles(Collections.singleton(userRole))
        .build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("getCurrentUserProfile should return UserViewDto for authenticated user")
  void getCurrentUserProfile_AuthenticatedUserFound() {
    // Arrange
    String userEmail = "current@example.com";
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getPrincipal()).thenReturn(userDetails); // Or directly use 'user' if User implements UserDetails correctly
    when(userDetails.getUsername()).thenReturn(userEmail); // Assuming username is email
    when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));

    // Act
    UserViewDto userProfile = userService.getCurrentUserProfile();

    // Assert
    assertNotNull(userProfile);
    assertEquals(user.getUserId(), userProfile.getId());
    assertEquals(user.getName(), userProfile.getName());
    assertEquals(user.getEmail(), userProfile.getEmail());
    assertEquals(user.getDateOfBirth(), userProfile.getDateOfBirth());
    assertEquals(user.getPlayingPosition(), userProfile.getPlayingPosition());
    assertThat(userProfile.getRoles()).containsExactly("ROLE_USER");

    // Verify
    verify(userRepository).findByEmail(userEmail);
  }

  @Test
  @DisplayName("getCurrentUserProfile should throw ResourceNotFoundException if authenticated user not in DB")
  void getCurrentUserProfile_AuthenticatedUserNotFoundInDb() {
    // Arrange
    String userEmail = "current@example.com";
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getPrincipal()).thenReturn(userDetails);
    when(userDetails.getUsername()).thenReturn(userEmail);
    when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty()); // User not found

    // Act & Assert
    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
      userService.getCurrentUserProfile();
    });

    assertEquals("User not found with email: " + userEmail, exception.getMessage());

    // Verify
    verify(userRepository).findByEmail(userEmail);
  }

  @Test
  @DisplayName("getCurrentUserProfile should throw IllegalStateException if no authenticated user")
  void getCurrentUserProfile_NoAuthenticatedUser() {
    // Arrange
    when(securityContext.getAuthentication()).thenReturn(null); // No authentication

    // Act & Assert
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      userService.getCurrentUserProfile();
    });

    assertEquals("No authenticated user found", exception.getMessage());

    // Verify
    verify(userRepository, never()).findByEmail(anyString()); // Repository should not be called
  }

  @Test
  @DisplayName("getCurrentUserProfile should throw IllegalStateException for anonymous user")
  void getCurrentUserProfile_AnonymousUser() {
    // Arrange
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isAuthenticated()).thenReturn(true); // Might be true for anonymous
    when(authentication.getPrincipal()).thenReturn("anonymousUser"); // Principal is String "anonymousUser"

    // Act & Assert
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      userService.getCurrentUserProfile();
    });

    assertEquals("No authenticated user found", exception.getMessage());

    // Verify
    verify(userRepository, never()).findByEmail(anyString());
  }
}