package com.nexora.platform.prompts;

import java.util.List;
import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/prompts")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping
    public List<PromptSummaryDto> list() {
        return promptService.listPrompts();
    }

    @PostMapping
    public PromptDetailDto create(@Valid @RequestBody CreatePromptRequest request) {
        return promptService.createPrompt(request);
    }

    @GetMapping("/{id}")
    public PromptDetailDto get(@PathVariable Long id) {
        return promptService.getPrompt(id);
    }

    @PostMapping("/{id}/versions")
    public PromptVersionDto addVersion(@PathVariable Long id, @Valid @RequestBody CreateVersionRequest request) {
        return promptService.addVersion(id, request);
    }

    @GetMapping("/{id}/runs")
    public List<PromptRunDto> runs(@PathVariable Long id) {
        return promptService.listRuns(id);
    }

    @PostMapping("/{id}/run")
    public PromptRunDto run(@PathVariable Long id, @RequestBody RunPromptRequest request) {
        return promptService.run(id, request);
    }

    @PutMapping("/runs/{runId}/rating")
    public PromptRunDto rate(@PathVariable Long runId, @Valid @RequestBody RatingRequest request) {
        return promptService.rate(runId, request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.notFound().build();
    }
}
