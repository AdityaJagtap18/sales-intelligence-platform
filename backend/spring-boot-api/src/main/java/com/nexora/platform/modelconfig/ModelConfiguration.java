package com.nexora.platform.modelconfig;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Single-row table (id always 1) holding the admin-selected override for
 * which locally-pulled Ollama chat model {@link
 * com.nexora.platform.prompts.OllamaChatClient} uses. The embedding model is
 * intentionally not overridable here — Qdrant's collection vector size is
 * fixed to nomic-embed-text's 768 dimensions, so swapping it would require
 * recreating the collection, not just a config change.
 */
@Entity
@Table(name = "model_configuration")
public class ModelConfiguration {

    @Id
    private Long id;

    @Column(name = "active_chat_model")
    private String activeChatModel;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ModelConfiguration() {
    }

    public ModelConfiguration(Long id, String activeChatModel, Instant updatedAt) {
        this.id = id;
        this.activeChatModel = activeChatModel;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getActiveChatModel() {
        return activeChatModel;
    }

    public void setActiveChatModel(String activeChatModel, Instant now) {
        this.activeChatModel = activeChatModel;
        this.updatedAt = now;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
