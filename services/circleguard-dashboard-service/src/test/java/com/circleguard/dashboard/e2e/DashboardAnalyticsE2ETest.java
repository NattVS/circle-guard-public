package com.circleguard.dashboard.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.circleguard.dashboard.client.PromotionClient;
import static org.mockito.Mockito.when;
import java.util.HashMap;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardAnalyticsE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromotionClient promotionClient;

    @Test
    void getCampusSummaryFlow() throws Exception {
        when(promotionClient.getHealthStats()).thenReturn(new HashMap<>());
        
        mockMvc.perform(get("/api/v1/dashboard/campus-summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
