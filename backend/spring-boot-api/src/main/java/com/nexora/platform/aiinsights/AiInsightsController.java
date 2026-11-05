package com.nexora.platform.aiinsights;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai-insights")
public class AiInsightsController {

    private final AiInsightsService service;

    public AiInsightsController(AiInsightsService service) {
        this.service = service;
    }

    @PostMapping("/ask")
    public AiInsightResponseDto ask(@Valid @RequestBody AskRequest request) {
        return service.ask(request.question());
    }

    @GetMapping("/history")
    public List<AiInsightHistoryDto> history() {
        return service.history().stream().map(AiInsightHistoryDto::from).toList();
    }
}
