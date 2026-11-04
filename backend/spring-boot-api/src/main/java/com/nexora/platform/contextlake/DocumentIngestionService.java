package com.nexora.platform.contextlake;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orchestrates a document upload: MinIO for the raw file (ADR-005: local
 * documents only, not analytical data), text extraction + chunking, and
 * embeddings into Qdrant for semantic search. Postgres only tracks metadata
 * and status — the chunk text/vectors live in Qdrant, not duplicated here.
 */
@Service
public class DocumentIngestionService {

    private final MinioDocumentStore documentStore;
    private final TextExtractor textExtractor;
    private final TextChunker textChunker;
    private final OllamaEmbeddingClient embeddingClient;
    private final QdrantClient qdrantClient;
    private final DocumentRepository repository;

    public DocumentIngestionService(MinioDocumentStore documentStore, TextExtractor textExtractor,
            TextChunker textChunker, OllamaEmbeddingClient embeddingClient, QdrantClient qdrantClient,
            DocumentRepository repository) {
        this.documentStore = documentStore;
        this.textExtractor = textExtractor;
        this.textChunker = textChunker;
        this.embeddingClient = embeddingClient;
        this.qdrantClient = qdrantClient;
        this.repository = repository;
    }

    public DocumentDto ingest(MultipartFile file) {
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        String objectKey = UUID.randomUUID() + "-" + file.getOriginalFilename();
        Document document = new Document(file.getOriginalFilename(), objectKey, contentType, file.getSize(),
                Instant.now());
        document = repository.save(document);

        try {
            byte[] content = file.getBytes();
            documentStore.upload(objectKey, content, contentType);

            String text = textExtractor.extract(contentType, content);
            List<String> chunks = textChunker.chunk(text);
            if (chunks.isEmpty()) {
                document.markFailed("No extractable text content");
                return DocumentDto.from(repository.save(document));
            }

            List<Map<String, Object>> points = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                List<Double> vector = embeddingClient.embed(chunks.get(i));
                points.add(Map.of(
                        "id", UUID.randomUUID().toString(),
                        "vector", vector,
                        "payload", Map.of(
                                "documentId", document.getId(),
                                "filename", document.getFilename(),
                                "chunkIndex", i,
                                "text", chunks.get(i))));
            }
            qdrantClient.upsert(points);

            document.markIndexed(chunks.size());
            return DocumentDto.from(repository.save(document));
        } catch (Exception e) {
            document.markFailed(e.getMessage());
            return DocumentDto.from(repository.save(document));
        }
    }
}
