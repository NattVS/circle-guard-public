package com.circleguard.identity.integration;

import com.circleguard.identity.service.IdentityVaultService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class IdentityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("circleguard_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update"); // O "validate" si usas Flyway en test
    }

    @Autowired
    private IdentityVaultService vaultService;

    @Test
    void contextLoads() {
        assertTrue(postgres.isRunning());
    }

    @Test
    void integration_SaveNewIdentityAndPersist() {
        String realIdentity = "DB_TEST|student@uni.edu";
        
        UUID anonymousId = vaultService.getOrCreateAnonymousId(realIdentity);
        
        assertNotNull(anonymousId);
    }

    @Test
    void integration_SaveAndResolveIdentity() {
        String realIdentity = "DB_TEST|teacher@uni.edu";
        
        UUID anonymousId = vaultService.getOrCreateAnonymousId(realIdentity);
        String resolved = vaultService.resolveRealIdentity(anonymousId);
        
        assertEquals(realIdentity, resolved);
    }

    @Test
    void integration_DuplicateIdentityReturnsSameUUID() {
        String realIdentity = "DB_TEST|duplicate@uni.edu";
        
        UUID firstCallId = vaultService.getOrCreateAnonymousId(realIdentity);
        UUID secondCallId = vaultService.getOrCreateAnonymousId(realIdentity);
        
        assertEquals(firstCallId, secondCallId);
    }

    @Test
    void integration_ResolveUnknownIdentityThrowsError() {
        UUID fakeId = UUID.randomUUID();
        
        assertThrows(ResponseStatusException.class, () -> {
            vaultService.resolveRealIdentity(fakeId);
        });
    }
}