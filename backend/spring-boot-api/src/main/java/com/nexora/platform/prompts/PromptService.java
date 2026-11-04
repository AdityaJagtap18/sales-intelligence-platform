package com.nexora.platform.prompts;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

@Service
public class PromptService {

    private final PromptRepository promptRepository;
    private final PromptVersionRepository versionRepository;
    private final PromptRunRepository runRepository;
    private final OllamaChatClient chatClient;

    public PromptService(PromptRepository promptRepository, PromptVersionRepository versionRepository,
            PromptRunRepository runRepository, OllamaChatClient chatClient) {
        this.promptRepository = promptRepository;
        this.versionRepository = versionRepository;
        this.runRepository = runRepository;
        this.chatClient = chatClient;
    }

    public List<PromptSummaryDto> listPrompts() {
        return promptRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(prompt -> toSummary(prompt, latestVersion(prompt.getId())))
                .toList();
    }

    public PromptDetailDto createPrompt(CreatePromptRequest request) {
        Instant now = Instant.now();
        Prompt prompt = promptRepository.save(new Prompt(request.name(), request.description(), now));
        PromptVersion version = versionRepository.save(new PromptVersion(prompt.getId(), 1, request.template(), now));
        return toDetail(prompt, List.of(version));
    }

    public PromptDetailDto getPrompt(Long promptId) {
        Prompt prompt = promptRepository.findById(promptId).orElseThrow(() -> notFound(promptId));
        return toDetail(prompt, versionRepository.findAllByPromptIdOrderByVersionNumberDesc(promptId));
    }

    public PromptVersionDto addVersion(Long promptId, CreateVersionRequest request) {
        Prompt prompt = promptRepository.findById(promptId).orElseThrow(() -> notFound(promptId));
        int nextVersion = latestVersion(prompt.getId()).map(v -> v.getVersionNumber() + 1).orElse(1);
        PromptVersion version = versionRepository.save(
                new PromptVersion(prompt.getId(), nextVersion, request.template(), Instant.now()));
        return PromptVersionDto.from(version);
    }

    public List<PromptRunDto> listRuns(Long promptId) {
        List<PromptVersion> versions = versionRepository.findAllByPromptIdOrderByVersionNumberDesc(promptId);
        if (versions.isEmpty()) {
            throw notFound(promptId);
        }
        Map<Long, Integer> versionNumberById = versions.stream()
                .collect(java.util.stream.Collectors.toMap(PromptVersion::getId, PromptVersion::getVersionNumber));
        List<Long> versionIds = versions.stream().map(PromptVersion::getId).toList();
        return runRepository.findAllByPromptVersionIdInOrderByCreatedAtDesc(versionIds).stream()
                .map(run -> toRunDto(run, versionNumberById.get(run.getPromptVersionId())))
                .toList();
    }

    public PromptRunDto run(Long promptId, RunPromptRequest request) {
        PromptVersion version = request.versionId() != null
                ? versionRepository.findById(request.versionId())
                        .filter(v -> v.getPromptId().equals(promptId))
                        .orElseThrow(() -> new NoSuchElementException("Unknown prompt version: " + request.versionId()))
                : latestVersion(promptId).orElseThrow(() -> notFound(promptId));

        String rendered = render(version.getTemplate(), request.input());
        PromptRun run = runRepository.save(new PromptRun(version.getId(), rendered, chatClient.chatModel(), Instant.now()));

        long startedAt = System.currentTimeMillis();
        try {
            String output = chatClient.generate(rendered);
            run.markSucceeded(output, System.currentTimeMillis() - startedAt);
        } catch (Exception e) {
            run.markFailed("Ollama generate call failed: " + e.getMessage());
        }
        runRepository.save(run);
        return toRunDto(run, version.getVersionNumber());
    }

    public PromptRunDto rate(Long runId, RatingRequest request) {
        PromptRun run = runRepository.findById(runId).orElseThrow(() -> new NoSuchElementException("Unknown run: " + runId));
        run.setRating(request.rating());
        runRepository.save(run);
        PromptVersion version = versionRepository.findById(run.getPromptVersionId()).orElseThrow();
        return toRunDto(run, version.getVersionNumber());
    }

    private String render(String template, String input) {
        if (template.contains("{{input}}")) {
            return template.replace("{{input}}", input == null ? "" : input);
        }
        if (input == null || input.isBlank()) {
            return template;
        }
        return template + "\n\n" + input;
    }

    private java.util.Optional<PromptVersion> latestVersion(Long promptId) {
        return versionRepository.findFirstByPromptIdOrderByVersionNumberDesc(promptId);
    }

    private PromptSummaryDto toSummary(Prompt prompt, java.util.Optional<PromptVersion> latest) {
        return new PromptSummaryDto(prompt.getId(), prompt.getName(), prompt.getDescription(),
                latest.map(PromptVersion::getVersionNumber).orElse(0), prompt.getCreatedAt());
    }

    private PromptDetailDto toDetail(Prompt prompt, List<PromptVersion> versions) {
        return new PromptDetailDto(prompt.getId(), prompt.getName(), prompt.getDescription(), prompt.getCreatedAt(),
                versions.stream().map(PromptVersionDto::from).toList());
    }

    private PromptRunDto toRunDto(PromptRun run, int versionNumber) {
        return new PromptRunDto(run.getId(), run.getPromptVersionId(), versionNumber, run.getRenderedPrompt(),
                run.getOutput(), run.getModel(), run.getRating(), run.getLatencyMs(), run.getErrorMessage(), run.getCreatedAt());
    }

    private NoSuchElementException notFound(Long promptId) {
        return new NoSuchElementException("Unknown prompt: " + promptId);
    }
}
