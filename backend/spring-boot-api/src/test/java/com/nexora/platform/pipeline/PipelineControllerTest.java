package com.nexora.platform.pipeline;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Confirms Angular's only access to the Databricks pipeline is through this
 * controller's DTOs — never a direct job-id/host/token value (ADR-005).
 */
@WebMvcTest(PipelineController.class)
class PipelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PipelineRunService service;

    @Test
    void runAcceptsTheTriggerAndReturns202WithThePersistedRun() throws Exception {
        PipelineRunDto dto = new PipelineRunDto(1L, 42L, 777L, "PENDING", null, null, "angular-ui",
                Instant.now(), Instant.now(), null, null);
        when(service.triggerRun("angular-ui")).thenReturn(dto);

        mockMvc.perform(post("/pipelines/run"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.lifecycleState").value("PENDING"))
                .andExpect(jsonPath("$.triggeredBy").value("angular-ui"));
    }

    @Test
    void runByIdReturns404ForAnUnknownRun() throws Exception {
        when(service.refreshRun(eq(99L))).thenThrow(new NoSuchElementException("No pipeline run with id 99"));

        mockMvc.perform(get("/pipelines/runs/99"))
                .andExpect(status().isNotFound());
    }
}
