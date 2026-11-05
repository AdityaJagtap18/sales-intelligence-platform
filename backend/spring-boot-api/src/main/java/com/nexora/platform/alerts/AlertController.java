package com.nexora.platform.alerts;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/observability/alerts")
public class AlertController {

    private final AlertRepository repository;

    public AlertController(AlertRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AlertDto> list(@RequestParam(required = false) String status,
            @RequestParam(required = false) String severity) {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .filter(a -> status == null || status.equalsIgnoreCase(a.getStatus()))
                .filter(a -> severity == null || severity.equalsIgnoreCase(a.getSeverity()))
                .map(AlertDto::from)
                .toList();
    }

    @PostMapping("/{id}/acknowledge")
    public AlertDto acknowledge(@PathVariable long id, @Valid @RequestBody AcknowledgeAlertRequest request) {
        Alert alert = find(id);
        alert.acknowledge(request.acknowledgedBy(), Instant.now());
        return AlertDto.from(repository.save(alert));
    }

    @PostMapping("/{id}/resolve")
    public AlertDto resolve(@PathVariable long id) {
        Alert alert = find(id);
        alert.resolve(Instant.now());
        return AlertDto.from(repository.save(alert));
    }

    private Alert find(long id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("No alert with id " + id));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.notFound().build();
    }
}
