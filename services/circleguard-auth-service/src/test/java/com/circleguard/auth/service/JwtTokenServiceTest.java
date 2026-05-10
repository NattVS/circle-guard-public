package com.circleguard.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService("test-secret-key-that-is-at-least-32-bytes-long", 3600000L);
    }

    @Test
    void generateToken_ReturnsValidJwtString() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "test", "test", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String token = jwtTokenService.generateToken(anonymousId, auth);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); 
    }
}
