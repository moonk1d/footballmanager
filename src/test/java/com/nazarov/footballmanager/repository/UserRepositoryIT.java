package com.nazarov.footballmanager.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.nazarov.footballmanager.domain.Role;
import com.nazarov.footballmanager.domain.User;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryIT {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private TestEntityManager entityManager;

  private Role userRole;

  @BeforeEach
  void setUp() {
    userRole = roleRepository.findByName("ROLE_USER")
        .orElseThrow(() -> new IllegalStateException("ROLE_USER not found, Flyway V2/V3 migration might have failed"));

     userRepository.deleteAll();
  }

  private User createUser(String email, String name) {
    User user = User.builder()
        .email(email)
        .name(name)
        .password("hashedPassword")
        .roles(Set.of(userRole))
        .build();
    return entityManager.persistAndFlush(user);
  }

  @Test
  @DisplayName("findByEmail should return user when email exists")
  void findByEmail_WhenEmailExists_ReturnsUser() {
    // Arrange
    String email = "findme@example.com";
    createUser(email, "Find Me");

    // Act
    Optional<User> foundUser = userRepository.findByEmail(email);

    // Assert
    assertThat(foundUser).isPresent();
    assertThat(foundUser.get().getEmail()).isEqualTo(email);
    assertThat(foundUser.get().getName()).isEqualTo("Find Me");
    assertThat(foundUser.get().getRoles()).contains(userRole);
  }

  @Test
  @DisplayName("findByEmail should return empty optional when email does not exist")
  void findByEmail_WhenEmailDoesNotExist_ReturnsEmpty() {
    // Arrange
    String email = "donotfindme@example.com";

    // Act
    Optional<User> foundUser = userRepository.findByEmail(email);

    // Assert
    assertThat(foundUser).isNotPresent();
  }

  @Test
  @DisplayName("existsByEmail should return true when email exists")
  void existsByEmail_WhenEmailExists_ReturnsTrue() {
    // Arrange
    String email = "exists@example.com";
    createUser(email, "Exists User");

    // Act
    boolean exists = userRepository.existsByEmail(email);

    // Assert
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("existsByEmail should return false when email does not exist")
  void existsByEmail_WhenEmailDoesNotExist_ReturnsFalse() {
    // Arrange
    String email = "doesnotexist@example.com";

    // Act
    boolean exists = userRepository.existsByEmail(email);

    // Assert
    assertThat(exists).isFalse();
  }

  @Test
  @DisplayName("save should persist user and assign ID")
  void save_ShouldPersistUserAndAssignId() {
    // Arrange
    User newUser = User.builder()
        .email("newuser@example.com")
        .name("New User")
        .password("hashedPassword")
        .roles(Set.of(userRole))
        .build();

    // Act
    User savedUser = userRepository.save(newUser);

    // Assert
    assertThat(savedUser).isNotNull();
    assertThat(savedUser.getUserId()).isNotNull().isPositive();
    assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");

    // Verify it's actually in the DB using TestEntityManager
    User foundInDb = entityManager.find(User.class, savedUser.getUserId());
    assertThat(foundInDb).isNotNull();
    assertThat(foundInDb.getEmail()).isEqualTo("newuser@example.com");
  }
}