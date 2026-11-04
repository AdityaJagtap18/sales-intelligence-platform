package com.nexora.platform.contextlake;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Context Lake document endpoints — Angular uploads and searches only through
 * here; MinIO/Qdrant/Ollama are never called directly from the browser.
 */
@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentIngestionService ingestionService;
    private final DocumentSearchService searchService;
    private final DocumentRepository repository;

    public DocumentController(DocumentIngestionService ingestionService, DocumentSearchService searchService,
            DocumentRepository repository) {
        this.ingestionService = ingestionService;
        this.searchService = searchService;
        this.repository = repository;
    }

    @PostMapping
    public DocumentDto upload(@RequestParam("file") MultipartFile file) {
        return ingestionService.ingest(file);
    }

    @GetMapping
    public List<DocumentDto> list() {
        return repository.findAllByOrderByUploadedAtDesc().stream().map(DocumentDto::from).toList();
    }

    @GetMapping("/search")
    public List<SearchResultDto> search(@RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return searchService.search(query, limit);
    }
}
