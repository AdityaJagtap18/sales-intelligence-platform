package com.nexora.platform.prompts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptServiceTest {

    @Mock
    private PromptRepository promptRepository;
    @Mock
    private PromptVersionRepository versionRepository;
    @Mock
    private PromptRunRepository runRepository;
    @Mock
    private OllamaChatClient chatClient;

    private PromptService service() {
        return new PromptService(promptRepository, versionRepository, runRepository, chatClient);
    }

    private PromptVersion versionWithTemplate(String template) {
        PromptVersion version = new PromptVersion(1L, 1, template, Instant.now());
        return version;
    }

    @Test
    void runSubstitutesInputIntoThePlaceholderWhenTemplateHasOne() {
        PromptVersion version = versionWithTemplate("Summarize: {{input}}");
        when(versionRepository.findFirstByPromptIdOrderByVersionNumberDesc(1L)).thenReturn(Optional.of(version));
        when(chatClient.chatModel()).thenReturn("llama3.2:1b");
        when(chatClient.generate(anyString())).thenReturn("a summary");
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().run(1L, new RunPromptRequest(null, "the quarterly report"));

        org.mockito.Mockito.verify(chatClient).generate("Summarize: the quarterly report");
    }

    @Test
    void runAppendsInputAfterTheTemplateWhenThereIsNoPlaceholder() {
        PromptVersion version = versionWithTemplate("You are a helpful assistant.");
        when(versionRepository.findFirstByPromptIdOrderByVersionNumberDesc(1L)).thenReturn(Optional.of(version));
        when(chatClient.chatModel()).thenReturn("llama3.2:1b");
        when(chatClient.generate(anyString())).thenReturn("ok");
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().run(1L, new RunPromptRequest(null, "hello"));

        org.mockito.Mockito.verify(chatClient).generate("You are a helpful assistant.\n\nhello");
    }

    @Test
    void runLeavesTemplateUnchangedWhenInputIsBlankAndThereIsNoPlaceholder() {
        PromptVersion version = versionWithTemplate("Fixed instructions only.");
        when(versionRepository.findFirstByPromptIdOrderByVersionNumberDesc(1L)).thenReturn(Optional.of(version));
        when(chatClient.chatModel()).thenReturn("llama3.2:1b");
        when(chatClient.generate(anyString())).thenReturn("ok");
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().run(1L, new RunPromptRequest(null, "  "));

        org.mockito.Mockito.verify(chatClient).generate("Fixed instructions only.");
    }

    @Test
    void runMarksTheRunFailedRatherThanThrowingWhenTheModelCallFails() {
        PromptVersion version = versionWithTemplate("{{input}}");
        when(versionRepository.findFirstByPromptIdOrderByVersionNumberDesc(1L)).thenReturn(Optional.of(version));
        when(chatClient.chatModel()).thenReturn("llama3.2:1b");
        when(chatClient.generate(anyString())).thenThrow(new RuntimeException("connection refused"));
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PromptRunDto result = service().run(1L, new RunPromptRequest(null, "hi"));

        assertThat(result.errorMessage()).contains("connection refused");
        assertThat(result.output()).isNull();
    }

    @Test
    void runThrowsWhenThePromptHasNoVersionsYet() {
        when(versionRepository.findFirstByPromptIdOrderByVersionNumberDesc(1L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class,
                () -> service().run(1L, new RunPromptRequest(null, "hi")));
    }

    @Test
    void addVersionIncrementsFromTheLatestExistingVersionNumber() throws Exception {
        Prompt prompt = new Prompt("Test", "desc", Instant.now());
        setId(prompt, 1L);
        when(promptRepository.findById(1L)).thenReturn(Optional.of(prompt));
        when(versionRepository.findFirstByPromptIdOrderByVersionNumberDesc(1L))
                .thenReturn(Optional.of(versionWithTemplate("v1 template")));
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PromptVersionDto result = service().addVersion(1L, new CreateVersionRequest("v2 template"));

        assertThat(result.versionNumber()).isEqualTo(2);
    }

    /** Prompt's id is normally assigned by JPA on save; set it directly for a detached test instance. */
    private static void setId(Prompt prompt, Long id) throws ReflectiveOperationException {
        java.lang.reflect.Field field = Prompt.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(prompt, id);
    }
}
