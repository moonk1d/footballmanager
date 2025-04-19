package com.nazarov.footballmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class FootballmanagerApplicationIT {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
			"postgres:17-alpine"
	);

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Test
	void contextLoads() {
		// If this test passes, it means:
		// 1. Testcontainers successfully started the PostgreSQL container (check Docker!).
		// 2. @DynamicPropertySource correctly configured Spring Boot's datasource properties.
		// 3. Spring Boot connected to the Testcontainers PostgreSQL instance.
		// 4. Flyway (if enabled in application-test.yml) ran migrations against the container's DB.
		// 5. The full Spring application context loaded successfully using the real DB connection.
		System.out.println("✅ Application Context Loaded Successfully with Testcontainers!");
		System.out.println("🔌 JDBC URL: " + postgres.getJdbcUrl());
	}

}