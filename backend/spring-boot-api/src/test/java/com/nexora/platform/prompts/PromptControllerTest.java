package com.nexora.platform.prompts;

import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PromptController.class)
class PromptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PromptService promptService;

    @Test
    void createRejectsABlankTemplateWithoutReachingTheService() throws Exception {
        mockMvc.perform(post("/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"description\":\"\",\"template\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReturnsTheCreatedPromptDetail() throws Exception {
        when(promptService.createPrompt(any())).thenReturn(
                new PromptDetailDto(1L, "Summarizer", "desc", Instant.now(), java.util.List.of()));

        mockMvc.perform(post("/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Summarizer\",\"description\":\"desc\",\"template\":\"{{input}}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Summarizer"));
    }

    @Test
    void getReturns404WhenThePromptServiceReportsItUnknown() throws Exception {
        when(promptService.getPrompt(eq(999L))).thenThrow(new NoSuchElementException("Unknown prompt: 999"));

        mockMvc.perform(get("/prompts/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rateRejectsARatingOutsideTheAllowedValues() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/prompts/runs/1/rating")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"MAYBE\"}"))
                .andExpect(status().isBadRequest());
    }
}
