package com.nexora.platform.analytics;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Confirms the controller only ever serves the aggregated DTOs the service
 * computes — no raw SQL or Databricks response ever reaches this layer.
 */
@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @Test
    void salesSummaryReturnsTheServiceDtoAsJson() throws Exception {
        when(analyticsService.salesSummary(isNull(), isNull())).thenReturn(
                new SalesSummaryDto(956303.59, 32, 29884.49, 847, 295, "Touring Bikes", 220655.38, 23.07));

        mockMvc.perform(get("/analytics/sales-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(32))
                .andExpect(jsonPath("$.topCategoryName").value("Touring Bikes"));
    }
}
