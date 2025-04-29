package com.nazarov.footballmanager.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nazarov.footballmanager.domain.Role;
import com.nazarov.footballmanager.domain.User;
import com.nazarov.footballmanager.repository.RoleRepository;
import com.nazarov.footballmanager.repository.UserRepository;
import com.nazarov.footballmanager.security.jwt.JwtTokenProvider;
import java.time.LocalDate;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class UserControllerIT {

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

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private User testUser;
  private String userJwtToken;

  @BeforeEach
  void setUp() {
    Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
    testUser = User.builder()
        .email("testuser@example.com")
        .name("Test Current User")
        .password(passwordEncoder.encode("password123"))
        .dateOfBirth(LocalDate.of(1995, 10, 20))
        .playingPosition("Midfielder")
        .roles(Set.of(userRole))
        .build();
    userRepository.save(testUser);

    userJwtToken = jwtTokenProvider.generateToken(testUser.getEmail());
  }

  @Test
  @DisplayName("GET /api/users/me - Authenticated User - Success")
  void getCurrentUser_WhenAuthenticated_ShouldReturnUserProfile() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(testUser.getUserId()))
        .andExpect(jsonPath("$.email").value(testUser.getEmail()))
        .andExpect(jsonPath("$.name").value(testUser.getName()))
        .andExpect(jsonPath("$.playingPosition").value("Midfielder"))
        .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
  }

  @Test
  @DisplayName("GET /api/users/me - Unauthenticated User")
  void getCurrentUser_WhenUnauthenticated_ShouldReturnUnauthorized() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/users/me")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /api/users/me - Invalid Token")
  void getCurrentUser_WhenInvalidToken_ShouldReturnUnauthorized() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + "invalid.token.here")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }
}