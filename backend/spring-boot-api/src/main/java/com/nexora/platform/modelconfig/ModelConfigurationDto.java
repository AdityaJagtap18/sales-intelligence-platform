package com.nexora.platform.modelconfig;

import java.time.Instant;
import java.util.List;

public record ModelConfigurationDto(
        String activeChatModel,
        String defaultChatModel,
        String embeddingModel,
        List<String> availableModels,
        String ollamaStatus,
        Instant updatedAt) {
}
