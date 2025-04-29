package com.nazarov.footballmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nazarov.footballmanager.domain.Role;
import com.nazarov.footballmanager.domain.User;
import com.nazarov.footballmanager.dto.user.JwtAuthenticationResponseDto;
import com.nazarov.footballmanager.dto.user.LoginRequestDto;
import com.nazarov.footballmanager.dto.user.UserRegistrationDto;
import com.nazarov.footballmanager.exception.BadRequestException;
import com.nazarov.footballmanager.exception.ResourceNotFoundException;
import com.nazarov.footballmanager.repository.RoleRepository;
import com.nazarov.footballmanager.repository.UserRepository;
import com.nazarov.footballmanager.security.jwt.JwtTokenProvider;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private AuthenticationManager authenticationManager;
  @Mock
  private UserRepository userRepository;
  @Mock
  private RoleRepository roleRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private JwtTokenProvider tokenProvider;

  @InjectMocks
  private AuthService authService;

  @Captor
  private ArgumentCaptor<User> userArgumentCaptor;
  @Captor
  private ArgumentCaptor<UsernamePasswordAuthenticationToken> authenticationTokenCaptor;

  private UserRegistrationDto registrationDto;
  private LoginRequestDto loginDto;
  private Role userRole;
  private User user;

  @BeforeEach
  void setUp() {
    // Common setup for tests
    registrationDto = UserRegistrationDto.builder()
        .name("Test User")
        .email("test@example.com")
        .password("password123")
        .build();

    loginDto = LoginRequestDto.builder()
        .email("test@example.com")
        .password("password123")
        .build();

    userRole = new Role(1, "ROLE_USER");

    user = User.builder()
        .userId(1)
        .name("Test User")
        .email("test@example.com")
        .password("encodedPassword")
        .roles(Collections.singleton(userRole))
        .build();
  }

  // --- Tests for registerUser ---

  @Test
  @DisplayName("registerUser should successfully register a new user")
  void registerUser_Success() {
    // Arrange: Mock dependencies
    when(userRepository.existsByEmail(registrationDto.getEmail())).thenReturn(false);
    when(passwordEncoder.encode(registrationDto.getPassword())).thenReturn("encodedPassword");
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
    // Mock the save operation to return the user with an ID
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User userToSave = invocation.getArgument(0);
      userToSave.setUserId(1);
      return userToSave;
    });

    // Act: Call the service method
    User registeredUser = authService.registerUser(registrationDto);

    // Assert: Verify interactions and results
    assertNotNull(registeredUser);
    assertEquals(registrationDto.getEmail(), registeredUser.getEmail());
    assertEquals("encodedPassword", registeredUser.getPassword());
    assertTrue(registeredUser.getRoles().contains(userRole));
    assertEquals(1, registeredUser.getUserId()); // Check if ID was assigned

    // Verify mock interactions
    verify(userRepository).existsByEmail(registrationDto.getEmail());
    verify(passwordEncoder).encode(registrationDto.getPassword());
    verify(roleRepository).findByName("ROLE_USER");
    verify(userRepository).save(userArgumentCaptor.capture());
  }

  @Test
  @DisplayName("registerUser should throw BadRequestException if email already exists")
  void registerUser_EmailExists() {
    // Arrange
    when(userRepository.existsByEmail(registrationDto.getEmail())).thenReturn(true);

    // Act & Assert
    BadRequestException exception = assertThrows(BadRequestException.class, () -> {
      authService.registerUser(registrationDto);
    });

    assertEquals("Email address already in use!", exception.getMessage());

    // Verify no further interactions happened
    verify(userRepository).existsByEmail(registrationDto.getEmail());
    verify(passwordEncoder, never()).encode(anyString());
    verify(roleRepository, never()).findByName(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("registerUser should throw ResourceNotFoundException if default role is missing")
  void registerUser_RoleNotFound() {
    // Arrange
    when(userRepository.existsByEmail(registrationDto.getEmail())).thenReturn(false);
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

    // Act & Assert
    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
      authService.registerUser(registrationDto);
    });

    assertEquals("Role not set: ROLE_USER", exception.getMessage());

    // Verify interactions
    verify(userRepository).existsByEmail(registrationDto.getEmail());
    verify(roleRepository).findByName("ROLE_USER");
    verify(passwordEncoder, never()).encode(anyString()); // Should not encode if role not found
    verify(userRepository, never()).save(any(User.class));
  }

  // --- Tests for loginUser ---

  @Test
  @DisplayName("loginUser should return JWT token on successful authentication")
  void loginUser_Success() {
    // Arrange
    String expectedToken = "mockJwtToken";
    Authentication authentication = mock(Authentication.class); // Mock the Authentication object returned by manager

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    when(tokenProvider.generateToken(authentication)).thenReturn(expectedToken);

    // Act
    JwtAuthenticationResponseDto response = authService.loginUser(loginDto);

    // Assert
    assertNotNull(response);
    assertEquals(expectedToken, response.getAccessToken());
    assertEquals("Bearer", response.getTokenType());

    // Verify mock interactions
    verify(authenticationManager).authenticate(authenticationTokenCaptor.capture());
    UsernamePasswordAuthenticationToken capturedToken = authenticationTokenCaptor.getValue();
    assertEquals(loginDto.getEmail(), capturedToken.getName());
    assertEquals(loginDto.getPassword(), capturedToken.getCredentials());

    verify(tokenProvider).generateToken(authentication);
  }

  @Test
  @DisplayName("loginUser should throw AuthenticationException on failed authentication")
  void loginUser_Failure() {
    // Arrange
    // Mock AuthenticationManager to throw an exception
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new AuthenticationException("Bad credentials") {}); // Use anonymous class or specific subclass

    // Act & Assert
    AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
      authService.loginUser(loginDto);
    });

    assertEquals("Bad credentials", exception.getMessage());

    // Verify mock interactions
    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(tokenProvider, never()).generateToken(any(Authentication.class)); // Token generation should not happen
  }
}