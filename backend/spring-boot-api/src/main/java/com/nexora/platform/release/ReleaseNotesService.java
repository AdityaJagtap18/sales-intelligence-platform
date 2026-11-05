package com.nexora.platform.release;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Serves the same history as the repo-root {@code CHANGELOG.md}/{@code VERSION}, condensed
 * to bullet form for the in-app "What's New" panel. Kept as a small static list rather than
 * parsing CHANGELOG.md at runtime -- that file isn't copied into the Docker image, and a
 * hand-written list is easier to keep accurate than a markdown parser. Bump
 * {@link #CURRENT_VERSION} and prepend an entry here whenever VERSION/CHANGELOG.md change.
 */
@Service
public class ReleaseNotesService {

    public static final String CURRENT_VERSION = "1.4.0";

    private static final List<ReleaseNoteDto> NOTES = List.of(
            new ReleaseNoteDto("1.4.0", "Production deployment: Hostinger VPS, GitHub Actions CI/CD",
                    List.of(
                            "Deployed to sellixai.adityajagtap.com (frontend) and sellixai-api.adityajagtap.com (API) with HTTPS.",
                            "Every push to master now tests, builds container images, and deploys automatically via GitHub Actions."),
                    List.of(), List.of()),
            new ReleaseNoteDto("1.3.2", "Rebrand: Nexora -> Sellix",
                    List.of(), List.of("Renamed the visible product name from Nexora to Sellix across the sidebar, browser tab title, and page title."),
                    List.of()),
            new ReleaseNoteDto("1.3.1", "Data Sources seeding + Forecasting hidden pending a usable dataset",
                    List.of("Data Sources now shows the real Databricks connection every other page already uses, instead of \"nothing registered\"."),
                    List.of("Forecasting is hidden from navigation and unreachable by URL — the sample dataset's single order date means it can only ever report \"insufficient data\". Code is kept, just unwired, pending a multi-date dataset or an order-sequence-based rework."),
                    List.of()),
            new ReleaseNoteDto("1.3.0", "Real functionality for previously-decorative topbar/toolbar controls",
                    List.of(
                            "Topbar Notifications bell now shows real open alerts with an unread-count badge, instead of doing nothing.",
                            "Topbar Help button links to this Release Notes page; the AI Assistant icon and search box now open AI Insights (search auto-asks your typed question).",
                            "Executive Overview's date-range chip is a real date-range picker wired to the sales queries (no visible effect on this single-date sample dataset, but genuinely wired end-to-end)."),
                    List.of("Removed the decorative \"Filters\" dropdown on Executive Overview, which had no real filter behind it."),
                    List.of()),
            new ReleaseNoteDto("1.2.0", "AI Insights live-data grounding + responsible-AI safeguards",
                    List.of(
                            "AI Insights answers are now grounded in a live Databricks sales-data snapshot (revenue, orders, category/product breakdowns), not only uploaded documents.",
                            "PII redaction (emails, phone numbers, SSNs, card numbers) applied before any document chunk or AI question/answer reaches Ollama, Qdrant, or a log.",
                            "Daily retention sweep purges AI Insights/Prompt Playground history older than 90 days."),
                    List.of("Switched the active chat model to qwen2.5:3b for noticeably more coherent answers."),
                    List.of()),
            new ReleaseNoteDto("1.1.4", "Fix: Ollama port collision with a native install",
                    List.of(), List.of(),
                    List.of("Docker's Ollama moved to port 11435 so it no longer silently loses port 11434 to a native Ollama install on the same machine.")),
            new ReleaseNoteDto("1.1.3", "Performance: cache Databricks SQL reads",
                    List.of("Databricks query results are now cached for 30s, cutting repeat page loads from ~780ms to ~2ms."),
                    List.of(), List.of()),
            new ReleaseNoteDto("1.1.2", "Fix: frontend/backend connectivity",
                    List.of(), List.of(),
                    List.of(
                            "Frontend was pointed at the wrong backend port.",
                            "The local dev server wasn't picking up Databricks credentials from .env.")),
            new ReleaseNoteDto("1.1.1", "Fix: dev server launch config",
                    List.of(), List.of(),
                    List.of("Fixed a path-resolution bug preventing the backend dev server from starting on Windows.")),
            new ReleaseNoteDto("1.1.0", "All remaining modules implemented",
                    List.of(
                            "Sales Dashboard, Reports, Customer Analytics, Forecasting, Anomaly Detection.",
                            "Data Sources, Data Pipelines, Data Quality.",
                            "System Health, Pipeline Monitoring, Logs & Traces, Alerts, Metrics.",
                            "AI Insights.",
                            "Settings, Integrations, Model Configuration, User Management."),
                    List.of(), List.of()),
            new ReleaseNoteDto("1.0.0", "Local MVP complete",
                    List.of("Full test suite, security review, and deployment guide."),
                    List.of(), List.of()));

    public List<ReleaseNoteDto> all() {
        return NOTES;
    }

    public String currentVersion() {
        return CURRENT_VERSION;
    }
}
