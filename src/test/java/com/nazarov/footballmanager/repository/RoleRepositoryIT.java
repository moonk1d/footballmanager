package com.nazarov.footballmanager.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.nazarov.footballmanager.domain.Role;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RoleRepositoryIT {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private RoleRepository roleRepository;

  @Test
  @DisplayName("findByName should return role when role exists")
  void findByName_WhenRoleExists_ReturnsRole() {
    // Arrange
    String existingRoleName = "ROLE_USER";

    // Act
    Optional<Role> foundRole = roleRepository.findByName(existingRoleName);

    // Assert
    assertThat(foundRole).isPresent();
    assertThat(foundRole.get().getName()).isEqualTo(existingRoleName);
    assertThat(foundRole.get().getId()).isNotNull();
  }

  @Test
  @DisplayName("findByName should return empty optional when role does not exist")
  void findByName_WhenRoleDoesNotExist_ReturnsEmpty() {
    // Arrange
    String nonExistentRoleName = "ROLE_GUEST";

    // Act
    Optional<Role> foundRole = roleRepository.findByName(nonExistentRoleName);

    // Assert
    assertThat(foundRole).isNotPresent();
  }

  @Test
  @DisplayName("findAll should return all seeded roles")
  void findAll_ShouldReturnSeededRoles() {
    // Act
    var roles = roleRepository.findAll();

    // Assert
    assertThat(roles).hasSize(3);
    assertThat(roles).extracting(Role::getName)
        .containsExactlyInAnyOrder("ROLE_USER", "ROLE_TEAM_MANAGER", "ROLE_ADMINISTRATOR");
  }
}