package com.nexora.platform.graph;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/graph")
public class GraphController {

    private final GraphSyncService syncService;
    private final GraphQueryService queryService;

    public GraphController(GraphSyncService syncService, GraphQueryService queryService) {
        this.syncService = syncService;
        this.queryService = queryService;
    }

    @PostMapping("/sync")
    public GraphSyncResultDto sync() {
        return syncService.sync();
    }

    @GetMapping("/stats")
    public GraphStatsDto stats() {
        return queryService.stats();
    }

    @GetMapping("/entities/{type}")
    public List<GraphEntityRefDto> entities(@PathVariable String type, @RequestParam(defaultValue = "50") int limit) {
        return queryService.listEntities(type, limit);
    }

    @GetMapping("/entity/{type}/{id}")
    public GraphTraversalDto entity(@PathVariable String type, @PathVariable String id,
            @RequestParam(defaultValue = "2") int depth) {
        return queryService.traverse(type, id, depth);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.notFound().build();
    }
}
