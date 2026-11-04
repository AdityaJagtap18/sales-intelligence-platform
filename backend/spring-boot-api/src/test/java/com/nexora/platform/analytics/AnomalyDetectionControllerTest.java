package com.nexora.platform.analytics;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Confirms the dedicated Anomaly Detection page reads the exact same
 * AnalyticsService#anomalies() computation Executive Overview uses, with
 * filtering and acknowledgement state layered on top.
 */
@WebMvcTest(AnomalyDetectionController.class)
class AnomalyDetectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private AnomalyAcknowledgementRepository ackRepository;

    @Test
    void listReturnsTheRankedAnomaliesFromTheSharedService() throws Exception {
        when(analyticsService.anomalies()).thenReturn(List.of(
                new AnomalyRowDto("order-value|SO71784", "Order Value", "SO71784", "High", "up", 82, 1000.0, 1820.0, null, null)));
        when(ackRepository.findByAnomalyKey("order-value|SO71784")).thenReturn(Optional.empty());

        mockMvc.perform(get("/analytics/anomalies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("SO71784"))
                .andExpect(jsonPath("$[0].impact").value("High"));
    }
}
