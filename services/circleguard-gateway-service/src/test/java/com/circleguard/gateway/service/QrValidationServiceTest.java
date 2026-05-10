package com.circleguard.gateway.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class QrValidationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private QrValidationService qrValidationService;

    private String validSecret = "test-secret-key-that-is-at-least-32-bytes-long";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(qrValidationService, "qrSecret", validSecret);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void validateToken_InvalidToken_ReturnsRed() {
        QrValidationService.ValidationResult result = qrValidationService.validateToken("invalid-token");

        assertFalse(result.valid());
        assertEquals("RED", result.status());
        assertEquals("Invalid or Expired Token", result.message());
    }

    @Test
    void validateToken_ValidToken_NoStatus_ReturnsGreen() {
        String token = generateTestToken(UUID.randomUUID().toString(), 3600000);
        
        QrValidationService.ValidationResult result = qrValidationService.validateToken(token);

        assertTrue(result.valid());
        assertEquals("GREEN", result.status());
    }

    @Test
    void validateToken_ValidToken_StatusContagied_ReturnsRed() {
        String id = UUID.randomUUID().toString();
        String token = generateTestToken(id, 3600000);
        
        when(valueOperations.get("user:status:" + id)).thenReturn("CONTAGIED");

        QrValidationService.ValidationResult result = qrValidationService.validateToken(token);

        assertFalse(result.valid());
        assertEquals("RED", result.status());
        assertTrue(result.message().contains("Risk Detected"));
    }

    @Test
    void validateToken_ValidToken_StatusPotential_ReturnsRed() {
        String id = UUID.randomUUID().toString();
        String token = generateTestToken(id, 3600000);
        
        when(valueOperations.get("user:status:" + id)).thenReturn("POTENTIAL");

        QrValidationService.ValidationResult result = qrValidationService.validateToken(token);

        assertFalse(result.valid());
        assertEquals("RED", result.status());
        assertTrue(result.message().contains("Risk Detected"));
    }

    @Test
    void validateToken_ValidToken_StatusCleared_ReturnsGreen() {
        String id = UUID.randomUUID().toString();
        String token = generateTestToken(id, 3600000);
        
        when(valueOperations.get("user:status:" + id)).thenReturn("CLEARED");

        QrValidationService.ValidationResult result = qrValidationService.validateToken(token);

        assertTrue(result.valid());
        assertEquals("GREEN", result.status());
    }

    private String generateTestToken(String subject, long expirationMs) {
        Key key = Keys.hmacShaKeyFor(validSecret.getBytes());
        return Jwts.builder()
                .setSubject(subject)
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
