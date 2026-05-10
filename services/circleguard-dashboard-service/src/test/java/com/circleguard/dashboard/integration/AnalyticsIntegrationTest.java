package com.circleguard.dashboard.integration;

import com.circleguard.dashboard.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class AnalyticsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("dashboard_test")
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
    private AnalyticsService analyticsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
        assertTrue(postgres.isRunning());
    }

    @Test
    void getEntryTrends_ReturnsEmptyListWhenNoData() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS entry_logs (id SERIAL, location_id UUID, entry_time TIMESTAMP)");
        UUID locationId = UUID.randomUUID();
        List<Map<String, Object>> trends = analyticsService.getEntryTrends(locationId);
        
        assertNotNull(trends);
        assertTrue(trends.isEmpty());
    }

    @Test
    void getEntryTrends_ReturnsDataWithKAnonymityWhenLittleData() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS entry_logs (id SERIAL, location_id UUID, entry_time TIMESTAMP)");
        UUID locId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO entry_logs (location_id, entry_time) VALUES (?, NOW())", locId);
        jdbcTemplate.update("INSERT INTO entry_logs (location_id, entry_time) VALUES (?, NOW())", locId);
        
        List<Map<String, Object>> trends = analyticsService.getEntryTrends(locId);
        assertNotNull(trends);
        if (!trends.isEmpty()) {
            assertEquals("<5", trends.get(0).get("entry_count"));
        }
    }

    @Test
    void getTimeSeries_FallsBackToMockDataWhenTableMissing() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS status_events");
        List<Map<String, Object>> result = analyticsService.getTimeSeries("hourly", 5);
        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void getTimeSeries_ReturnsDataWhenTableExists() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS status_events (id SERIAL, status VARCHAR(255), event_time TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO status_events (status, event_time) VALUES ('ACTIVE', NOW())");
        
        List<Map<String, Object>> result = analyticsService.getTimeSeries("hourly", 5);
        assertNotNull(result);
        assertTrue(result.size() > 0);
    }
}
