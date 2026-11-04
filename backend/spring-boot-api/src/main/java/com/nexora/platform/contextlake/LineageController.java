package com.nexora.platform.contextlake;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lineage")
public class LineageController {

    private final LineageService lineageService;

    public LineageController(LineageService lineageService) {
        this.lineageService = lineageService;
    }

    @GetMapping("/tables")
    public List<String> tables() {
        return LineageService.SALESLT_TABLES;
    }

    @GetMapping("/{table}")
    public LineageDto lineage(@PathVariable String table) {
        return lineageService.lineageFor(table);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.notFound().build();
    }
}
