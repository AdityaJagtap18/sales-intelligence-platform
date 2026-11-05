package com.nexora.platform.aiinsights;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.nexora.platform.contextlake.DocumentRepository;
import com.nexora.platform.contextlake.DocumentSearchService;
import com.nexora.platform.contextlake.SearchResultDto;
import com.nexora.platform.graph.GraphNodeDto;
import com.nexora.platform.graph.GraphQueryService;
import com.nexora.platform.graph.GraphTraversalDto;
import com.nexora.platform.prompts.OllamaChatClient;

/**
 * Grounded AI Insights (/intelligence/ai-insights): every answer is
 * generated only from Context Lake chunks actually retrieved for the
 * question ({@link DocumentSearchService}, reused rather than a second
 * embedding/search pipeline), and every citation is a real document chunk
 * with its real similarity score. When nothing relevant is indexed, this
 * returns an honest INSUFFICIENT_CONTEXT result instead of asking the model
 * to answer from its own unsourced knowledge.
 */
@Service
public class AiInsightsService {

    private static final Logger log = LoggerFactory.getLogger(AiInsightsService.class);
    private static final int TOP_K = 5;
    private static final double MIN_RELEVANCE_SCORE = 0.3;

    private final DocumentSearchService searchService;
    private final DocumentRepository documentRepository;
    private final OllamaChatClient chatClient;
    private final GraphQueryService graphQueryService;
    private final AiInsightRunRepository runRepository;

    public AiInsightsService(DocumentSearchService searchService, DocumentRepository documentRepository,
            OllamaChatClient chatClient, GraphQueryService graphQueryService, AiInsightRunRepository runRepository) {
        this.searchService = searchService;
        this.documentRepository = documentRepository;
        this.chatClient = chatClient;
        this.graphQueryService = graphQueryService;
        this.runRepository = runRepository;
    }

    public AiInsightResponseDto ask(String question) {
        Instant now = Instant.now();

        if (documentRepository.findAllByOrderByUploadedAtDesc().stream()
                .noneMatch(d -> "INDEXED".equals(d.getStatus()))) {
            return persist(question, null, List.of(), "NO_SOURCES",
                    "No documents are indexed in the Context Lake yet. Upload and index at least one document "
                            + "under Data Platform > Context Lake before asking questions here.",
                    now);
        }

        List<SearchResultDto> results = searchService.search(question, TOP_K);
        List<SearchResultDto> relevant = results.stream().filter(r -> r.score() >= MIN_RELEVANCE_SCORE).toList();

        if (relevant.isEmpty()) {
            return persist(question, null, List.of(), "INSUFFICIENT_CONTEXT",
                    "Nothing indexed in the Context Lake is relevant enough to this question (best match scored "
                            + (results.isEmpty() ? "n/a" : String.format("%.2f", results.get(0).score()))
                            + ", below the " + MIN_RELEVANCE_SCORE + " threshold). Try rephrasing, or index more"
                            + " relevant documents.",
                    now);
        }

        String prompt = buildPrompt(question, relevant);
        String answer;
        try {
            answer = chatClient.generate(prompt);
        } catch (Exception e) {
            log.warn("AI Insights: Ollama generation failed: {}", e.getMessage());
            return persist(question, null, List.of(), "GENERATION_FAILED",
                    "The local Ollama model (" + chatClient.chatModel() + ") is unreachable or failed to respond: "
                            + e.getMessage(),
                    now);
        }

        List<CitationDto> citations = relevant.stream()
                .map(r -> new CitationDto(r.documentId(), r.filename(), r.chunkIndex(), round(r.score()),
                        snippet(r.text()), relatedEntities(r.documentId())))
                .toList();

        return persist(question, answer, citations, "ANSWERED", null, now);
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
