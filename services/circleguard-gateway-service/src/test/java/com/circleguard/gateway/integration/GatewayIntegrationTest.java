package com.circleguard.gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@Testcontainers
class GatewayIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
        registry.add("qr.secret", () -> "test-secret-key-that-is-at-least-32-bytes-long");
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void contextLoads() {
        assertTrue(redis.isRunning());
    }

    @Test
    void redisConnection_SetAndGet() {
        redisTemplate.opsForValue().set("testKey", "testValue");
        String value = redisTemplate.opsForValue().get("testKey");
        assertEquals("testValue", value);
    }

    @Test
    void redisConnection_DeleteKey() {
        redisTemplate.opsForValue().set("deleteKey", "val");
        redisTemplate.delete("deleteKey");
        assertNull(redisTemplate.opsForValue().get("deleteKey"));
    }

    @Test
    void redisConnection_KeyExpiration() throws InterruptedException {
        redisTemplate.opsForValue().set("expireKey", "val", 1, TimeUnit.SECONDS);
        Thread.sleep(1500);
        assertNull(redisTemplate.opsForValue().get("expireKey"));
    }

    @Test
    void redisConnection_HasKey() {
        redisTemplate.opsForValue().set("hasKey", "val");
        assertTrue(redisTemplate.hasKey("hasKey"));
        assertFalse(redisTemplate.hasKey("notExist"));
    }
}
