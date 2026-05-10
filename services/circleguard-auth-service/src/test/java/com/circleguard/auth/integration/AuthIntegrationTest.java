package com.circleguard.auth.integration;

import com.circleguard.auth.model.LocalUser;
import com.circleguard.auth.repository.LocalUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private LocalUserRepository userRepository;

    @Test
    void contextLoads() {
        assertTrue(postgres.isRunning());
    }

    @Test
    void findByUsername_ReturnsEmptyWhenNotExists() {
        Optional<LocalUser> user = userRepository.findByUsername("nonexistent");
        assertTrue(user.isEmpty());
    }

    @Test
    void saveUser_ThenFindById() {
        LocalUser user = new LocalUser();
        user.setUsername("testuser1");
        user.setPassword("pass");
        user.setIsActive(true);
        LocalUser saved = userRepository.save(user);

        Optional<LocalUser> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("testuser1", found.get().getUsername());
    }

    @Test
    void findByUsername_ReturnsUserWhenExists() {
        LocalUser user = new LocalUser();
        user.setUsername("testuser2");
        user.setPassword("pass");
        user.setIsActive(true);
        userRepository.save(user);

        Optional<LocalUser> found = userRepository.findByUsername("testuser2");
        assertTrue(found.isPresent());
        assertEquals("testuser2", found.get().getUsername());
    }

    @Test
    void deleteUser_ThenNotFound() {
        LocalUser user = new LocalUser();
        user.setUsername("testuser3");
        user.setPassword("pass");
        user.setIsActive(true);
        LocalUser saved = userRepository.save(user);

        userRepository.deleteById(saved.getId());
        Optional<LocalUser> found = userRepository.findById(saved.getId());
        assertTrue(found.isEmpty());
    }
}
