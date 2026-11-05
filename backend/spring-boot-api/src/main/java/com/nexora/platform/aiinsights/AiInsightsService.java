package com.nexora.platform.aiinsights;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.nexora.platform.analytics.AnalyticsService;
import com.nexora.platform.analytics.LabelValueDto;
import com.nexora.platform.analytics.ProductPerformanceDto;
import com.nexora.platform.analytics.SalesSummaryDto;
import com.nexora.platform.contextlake.DocumentRepository;
import com.nexora.platform.contextlake.DocumentSearchService;
import com.nexora.platform.contextlake.SearchResultDto;
import com.nexora.platform.graph.GraphNodeDto;
import com.nexora.platform.graph.GraphQueryService;
import com.nexora.platform.graph.GraphTraversalDto;
import com.nexora.platform.privacy.PiiRedactor;
import com.nexora.platform.prompts.OllamaChatClient;

/**
 * Grounded AI Insights (/intelligence/ai-insights): every answer is
 * generated only from sources actually retrieved for the question --
 * Context Lake chunks ({@link DocumentSearchService}, reused rather than a
 * second embedding/search pipeline) plus a live aggregate snapshot of
 * {@link AnalyticsService}'s Databricks Gold-layer data, so a question like
 * "what was June's revenue" is answered from the real current figure
 * instead of only whatever happens to be in an uploaded document. Every
 * citation is a real source with its real score. The live snapshot is
 * deliberately built from aggregate business metrics only (totals, category
 * and product breakdowns) -- never per-customer rows -- so no personal data
 * (GDPR Art. 5(1)(c) data minimisation) reaches the Ollama prompt or the
 * citations shown back to the user. When nothing relevant is available at
 * all, this returns an honest INSUFFICIENT_CONTEXT result instead of asking
 * the model to answer from its own unsourced knowledge.
 */
@Service
public class AiInsightsService {

    private static final Logger log = LoggerFactory.getLogger(AiInsightsService.class);
    private static final int TOP_K = 5;
    private static final double MIN_RELEVANCE_SCORE = 0.3;
    private static final String LIVE_DATA_SOURCE_NAME = "Live Sales Data (Databricks workspace.gold.*)";

    private final DocumentSearchService searchService;
    private final DocumentRepository documentRepository;
    private final OllamaChatClient chatClient;
    private final GraphQueryService graphQueryService;
    private final AiInsightRunRepository runRepository;
    private final AnalyticsService analyticsService;

    public AiInsightsService(DocumentSearchService searchService, DocumentRepository documentRepository,
            OllamaChatClient chatClient, GraphQueryService graphQueryService, AiInsightRunRepository runRepository,
            AnalyticsService analyticsService) {
        this.searchService = searchService;
        this.documentRepository = documentRepository;
        this.chatClient = chatClient;
        this.graphQueryService = graphQueryService;
        this.runRepository = runRepository;
        this.analyticsService = analyticsService;
    }

    public AiInsightResponseDto ask(String rawQuestion) {
        Instant now = Instant.now();
        // Redacted before it's embedded for search, sent to Ollama, or persisted to
        // the ai_insight_run audit table -- a question is free text the user typed
        // and may accidentally contain personal data (GDPR data minimisation).
        String question = PiiRedactor.redact(rawQuestion);

        List<SearchResultDto> relevant = new ArrayList<>();
        SearchResultDto liveData = liveDataSource();
        if (liveData != null) {
            relevant.add(liveData);
        }

        boolean hasIndexedDocs = documentRepository.findAllByOrderByUploadedAtDesc().stream()
                .anyMatch(d -> "INDEXED".equals(d.getStatus()));
        double bestDocScore = 0;
        if (hasIndexedDocs) {
            List<SearchResultDto> results = searchService.search(question, TOP_K);
            bestDocScore = results.isEmpty() ? 0 : results.get(0).score();
            relevant.addAll(results.stream().filter(r -> r.score() >= MIN_RELEVANCE_SCORE).toList());
        }

        if (relevant.isEmpty()) {
            String note = hasIndexedDocs
                    ? "Nothing indexed in the Context Lake is relevant enough to this question (best match scored "
                            + String.format("%.2f", bestDocScore) + ", below the " + MIN_RELEVANCE_SCORE
                            + " threshold), and the live Databricks sales data is currently unreachable. Try"
                            + " rephrasing, or check System Health."
                    : "No documents are indexed in the Context Lake, and the live Databricks sales data is"
                            + " currently unreachable. Upload a document under Data Platform > Context Lake, or"
                            + " check System Health for the Databricks SQL Warehouse.";
            return persist(question, null, List.of(), hasIndexedDocs ? "INSUFFICIENT_CONTEXT" : "NO_SOURCES", note,
                    now);
        }

        String prompt = buildPrompt(question, relevant);
        String answer;
        try {
            answer = PiiRedactor.redact(chatClient.generate(prompt));
        } catch (Exception e) {
            log.warn("AI Insights: Ollama generation failed: {}", e.getMessage());
            return persist(question, null, List.of(), "GENERATION_FAILED",
                    "The local Ollama model (" + chatClient.chatModel() + ") is unreachable or failed to respond: "
                            + e.getMessage(),
                    now);
        }

        List<CitationDto> citations = relevant.stream()
                .map(r -> new CitationDto(r.documentId(), r.filename(), r.chunkIndex(), round(r.score()),
                        snippet(r.text()), r.documentId() <= 0 ? List.of() : relatedEntities(r.documentId())))
                .toList();

        return persist(question, answer, citations, "ANSWERED", null, now);
    }

    /**
     * A compact, always-fresh snapshot of aggregate Gold-layer metrics
     * (served from {@link com.nexora.platform.databricks.DatabricksSqlClient}'s
     * own cache on repeat questions). Returns null rather than throwing when
     * Databricks is unreachable, so a sales-data outage degrades AI Insights
     * to document-only grounding instead of failing the whole question.
     */
    private SearchResultDto liveDataSource() {
        try {
            SalesSummaryDto summary = analyticsService.salesSummary();
            List<LabelValueDto> categories = analyticsService.revenueByCategory();
            List<ProductPerformanceDto> products = analyticsService.productPerformance();

            StringBuilder sb = new StringBuilder(String.format(
                    "This sample dataset (AdventureWorksLT) contains orders from a single period, June 2008 --"
                            + " so these totals ARE the figures for June 2008, the only period in this data."
                            + " Aggregate business metrics only, no individual customer data: total revenue $%.2f"
                            + " across %d orders, %d customers, %d products in catalog. Average order value $%.2f."
                            + " Top category by revenue: %s (%.1f%% of revenue).",
                    summary.totalRevenue(), summary.totalOrders(), summary.totalCustomers(), summary.totalProducts(),
                    summary.avgOrderValue(), summary.topCategoryName(), summary.topCategorySharePct()));
            if (!categories.isEmpty()) {
                sb.append(" Revenue share by category: ").append(categories.stream().limit(8)
                        .map(c -> c.label() + " " + c.value() + "%").collect(Collectors.joining(", "))).append('.');
            }
            if (!products.isEmpty()) {
                sb.append(" Top-selling products by revenue: ").append(products.stream().limit(8)
                        .map(p -> p.name() + " ($" + String.format("%.2f", p.revenue()) + ")")
                        .collect(Collectors.joining(", "))).append('.');
            }
            return new SearchResultDto(0, LIVE_DATA_SOURCE_NAME, -1, sb.toString(), 1.0);
        } catch (Exception e) {
            log.debug("AI Insights: live Databricks data unavailable, falling back to document-only grounding: {}",
                    e.getMessage());
            return null;
        }
    }

    public List<AiInsightRun> history() {
        return runRepository.findAllByOrderByAskedAtDesc();
    }

    private String buildPrompt(String question, List<SearchResultDto> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Answer the question using ONLY the numbered sources below. ")
                .append("If the sources do not contain the answer, say so plainly instead of guessing. ")
                .append("Reference sources by their number, e.g. [1].\n\n");
        for (int i = 0; i < context.size(); i++) {
            SearchResultDto r = context.get(i);
            sb.append('[').append(i + 1).append("] (").append(r.filename()).append(") ").append(r.text()).append("\n\n");
        }
        sb.append("Question: ").append(question).append("\nAnswer:");
        return sb.toString();
    }

    private List<RelatedEntityDto> relatedEntities(long documentId) {
        try {
            GraphTraversalDto traversal = graphQueryService.traverse("document", String.valueOf(documentId), 1);
            return traversal.nodes().stream()
                    .filter(n -> !"Document".equals(n.label()))
                    .map(this::toRelatedEntity)
                    .distinct()
                    .toList();
        } catch (NoSuchElementException e) {
            return List.of();
        } catch (Exception e) {
            log.debug("AI Insights: graph lookup for document {} failed: {}", documentId, e.getMessage());
            return List.of();
        }
    }

    private RelatedEntityDto toRelatedEntity(GraphNodeDto node) {
        Object name = node.properties().getOrDefault("name",
                node.properties().getOrDefault("companyName", node.properties().get("filename")));
        return new RelatedEntityDto(node.label(), name == null ? node.id() : name.toString());
    }

    private String snippet(String text) {
        return text.length() > 240 ? text.substring(0, 240) + "…" : text;
    }

    private double round(double score) {
        return Math.round(score * 1000) / 1000.0;
    }

    private AiInsightResponseDto persist(String question, String answer, List<CitationDto> citations, String status,
            String note, Instant now) {
        AiInsightRun run = runRepository.save(new AiInsightRun(question, answer, status, citations.size(), now));
        return new AiInsightResponseDto(run.getId(), question, answer, citations, status, note);
    }
}
