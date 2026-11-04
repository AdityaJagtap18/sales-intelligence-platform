# ADR-003: Ollama for local LLM inference

## Status
Accepted

## Context
Modules F (Prompt Library), G (AI Insights) and parts of Module D (document embeddings) need LLM inference and embeddings without depending on a paid hosted API, consistent with ADR-001's local-first principle.

## Decision
Use Ollama as the local inference runtime for both chat/completion models and embedding models. All calls go through a thin provider interface (not scattered `ollama` SDK calls) so a hosted provider can be added later without touching calling code.

## Consequences
- No API keys or egress required for core AI features to work.
- Model quality/speed is bounded by what runs on the local machine — acceptable for an MVP demonstrating the integration pattern, not a production SLA.
- The provider interface must not grow speculative multi-provider configuration before a second provider is actually needed (per engineering principle #16, avoid unnecessary abstraction) — it starts as a single Ollama implementation behind one interface, nothing more.
- Grounding rules from spec §Module G (cite sources, distinguish retrieved facts from generated text, state when data is unavailable) are enforced in the calling code (Spring Boot/Python), not assumed from the model.
