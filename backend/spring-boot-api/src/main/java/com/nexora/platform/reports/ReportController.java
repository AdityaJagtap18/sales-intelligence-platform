package com.nexora.platform.reports;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/definitions")
    public List<ReportDefinitionDto> definitions() {
        return service.definitions();
    }

    @PostMapping("/{key}/generate")
    public ReportResultDto generate(@org.springframework.web.bind.annotation.PathVariable String key,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String category, @RequestParam(required = false) String territory) {
        return service.generate(key, from, to, category, territory);
    }

    @GetMapping("/history")
    public List<ReportExecutionDto> history() {
        return service.history();
    }

    @GetMapping(value = "/{key}/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(@org.springframework.web.bind.annotation.PathVariable String key,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String category, @RequestParam(required = false) String territory) {
        ReportResultDto result = service.generate(key, from, to, category, territory);
        StringBuilder csv = new StringBuilder(String.join(",", result.columns())).append('\n');
        for (Map<String, Object> row : result.rows()) {
            csv.append(String.join(",", result.columns().stream().map(c -> csvField(row.get(c))).toList())).append('\n');
        }
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(key.toLowerCase() + "-report.csv").build().toString())
                .body(body);
    }

    private static String csvField(Object value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.toString().replace("\"", "\"\"") + "\"";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
