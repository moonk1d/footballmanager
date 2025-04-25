package com.nazarov.footballmanager.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nazarov.footballmanager.domain.User;
import com.nazarov.footballmanager.dto.user.LoginRequestDto;
import com.nazarov.footballmanager.dto.user.UserRegistrationDto;
import com.nazarov.footballmanager.repository.RoleRepository;
import com.nazarov.footballmanager.repository.UserRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class AuthControllerIT {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;
  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private UserRegistrationDto validRegistrationDto;
  private UserRegistrationDto invalidRegistrationDto;
  private LoginRequestDto validLoginDto;
  private LoginRequestDto invalidLoginDto;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();

    validRegistrationDto = new UserRegistrationDto();
    validRegistrationDto.setName("Test User Reg");
    validRegistrationDto.setEmail("register@example.com");
    validRegistrationDto.setPassword("password123");
    validRegistrationDto.setDateOfBirth("1999-01-01");

    invalidRegistrationDto = new UserRegistrationDto();
    invalidRegistrationDto.setName("Test User");
    invalidRegistrationDto.setEmail("");
    invalidRegistrationDto.setPassword("password123");

    validLoginDto = new LoginRequestDto();
    validLoginDto.setEmail("login@example.com");
    validLoginDto.setPassword("password123");

    invalidLoginDto = new LoginRequestDto();
    invalidLoginDto.setEmail("login@example.com");
    invalidLoginDto.setPassword("wrongpassword");
  }

  @Test
  @DisplayName("POST /api/auth/register - Success")
  void registerUser_WhenValidInput_ShouldReturnCreatedAndSaveUser() throws Exception {
    // Act
    ResultActions result = mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRegistrationDto)));

    var savedUserOpt = userRepository.findByEmail(validRegistrationDto.getEmail());
    assertThat(savedUserOpt).isPresent();
    User savedUser = savedUserOpt.get();

    // Assert - Response
    result.andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION, "/api/users/" + savedUser.getUserId()))
        .andExpect(content().string("User registered successfully"));
  }

  @Test
  @DisplayName("POST /api/auth/register - Validation Failure (Blank Email)")
  void registerUser_WhenEmailIsBlank_ShouldReturnBadRequest() throws Exception {
    // Act & Assert
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRegistrationDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.email").value("Email cannot be blank"));
  }

  @Test
  @DisplayName("POST /api/auth/register - Validation Failure (Invalid Email Format)")
  void registerUser_WhenEmailIsInvalid_ShouldReturnBadRequest() throws Exception {
    // Arrange
    invalidRegistrationDto.setEmail("invalid-email");

    // Act & Assert
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRegistrationDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.email").value("Invalid email format"));
  }

  @Test
  @DisplayName("POST /api/auth/register - Validation Failure (Invalid Date of Birth)")
  void registerUser_WhenDateOfBirthIsInvalid_ShouldReturnBadRequest() throws Exception {
    // Arrange
    invalidRegistrationDto.setDateOfBirth("invalid-date");

    // Act & Assert
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRegistrationDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.dateOfBirth").value("Date of birth must be in YYYY-MM-DD format"));
  }

  @Test
  @DisplayName("POST /api/auth/register - Email Already Exists")
  void registerUser_WhenEmailExists_ShouldReturnBadRequest() throws Exception {
    userRepository.save(User.builder()
        .email(validRegistrationDto.getEmail())
        .name("Existing User")
        .password(passwordEncoder.encode("somepassword"))
        .roles(Set.of(roleRepository.findByName("ROLE_USER").orElseThrow())) // Assuming role exists
        .build());

    // Act & Assert
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRegistrationDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Email address already in use!"));
  }

  @Test
  @DisplayName("POST /api/auth/login - Success")
  void authenticateUser_WhenValidCredentials_ShouldReturnOkWithToken() throws Exception {
    // Arrange - Create the user to log in
    userRepository.save(User.builder()
        .email(validLoginDto.getEmail())
        .name("Login User")
        .password(passwordEncoder.encode(validLoginDto.getPassword()))
        .roles(Set.of(roleRepository.findByName("ROLE_USER").orElseThrow()))
        .build());

    // Act & Assert
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validLoginDto)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andReturn();
  }

  @Test
  @DisplayName("POST /api/auth/login - Invalid Credentials (Wrong Password)")
  void authenticateUser_WhenInvalidPassword_ShouldReturnUnauthorized() throws Exception {
    // Arrange - Create the user with the correct password
    userRepository.save(User.builder()
        .email(invalidLoginDto.getEmail())
        .name("Login User")
        .password(passwordEncoder.encode("password123"))
        .roles(Set.of(roleRepository.findByName("ROLE_USER").orElseThrow()))
        .build());

    // Act & Assert
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidLoginDto)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Bad credentials"));
  }

  @Test
  @DisplayName("POST /api/auth/login - Invalid Credentials (User Not Found)")
  void authenticateUser_WhenUserNotFound_ShouldReturnUnauthorized() throws Exception {
    // Act & Assert
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validLoginDto)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value(
            "Bad credentials"));
  }
}