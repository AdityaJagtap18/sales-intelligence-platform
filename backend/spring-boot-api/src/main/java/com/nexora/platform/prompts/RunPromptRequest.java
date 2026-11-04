package com.nexora.platform.prompts;

/** {@code versionId} null means "run the prompt's latest version". */
public record RunPromptRequest(Long versionId, String input) {
}
