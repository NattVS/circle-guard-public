package com.circleguard.dashboard.service;

import com.circleguard.dashboard.client.PromotionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private PromotionClient promotionClient;

    @Mock
    private KAnonymityFilter kAnonymityFilter;

    @InjectMocks
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getCampusSummary_ReturnsExpectedMap() {
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put("activeCases", 10);
        when(promotionClient.getHealthStats()).thenReturn(expectedStats);

        Map<String, Object> result = analyticsService.getCampusSummary();

        assertEquals(10, result.get("activeCases"));
    }

    @Test
    void getDepartmentStats_AppliesKAnonymity() {
        Map<String, Object> rawStats = new HashMap<>();
        rawStats.put("cases", 3);
        
        Map<String, Object> anonymizedStats = new HashMap<>();
        anonymizedStats.put("cases", "<5");
        
        when(promotionClient.getHealthStatsByDepartment("CS")).thenReturn(rawStats);
        when(kAnonymityFilter.apply(rawStats)).thenReturn(anonymizedStats);

        Map<String, Object> result = analyticsService.getDepartmentStats("CS");

        assertEquals("<5", result.get("cases"));
    }

    @Test
    void getEntryTrends_AppliesKAnonymityToRows() {
        UUID locId = UUID.randomUUID();
        Map<String, Object> row = new HashMap<>();
        row.put("hour", new Date());
        row.put("entry_count", 3L);
        
        when(jdbc.queryForList(anyString(), org.mockito.ArgumentMatchers.eq(locId)))
                .thenReturn(List.of(row));

        List<Map<String, Object>> result = analyticsService.getEntryTrends(locId);

        assertEquals(1, result.size());
        assertEquals("<5", result.get(0).get("entry_count"));
        assertEquals("Insufficient data for privacy", result.get(0).get("note"));
    }

    @Test
    void getGlobalHealthStats_ReturnsCampusSummary() {
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put("global", 100);
        when(promotionClient.getHealthStats()).thenReturn(expectedStats);

        Map<String, Object> result = analyticsService.getGlobalHealthStats();

        assertEquals(100, result.get("global"));
    }

    @Test
    void getTimeSeries_FallbackToMock_WhenException() {
        when(jdbc.queryForList(anyString(), anyInt())).thenThrow(new RuntimeException("Table missing"));

        List<Map<String, Object>> result = analyticsService.getTimeSeries("hourly", 5);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }
}
