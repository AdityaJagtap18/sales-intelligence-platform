package com.nexora.platform.analytics;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics/customers")
public class CustomerAnalyticsController {

    private final CustomerAnalyticsService service;

    public CustomerAnalyticsController(CustomerAnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public CustomerSummaryDto summary() {
        return service.summary();
    }

    @GetMapping("/geographic-distribution")
    public List<LabelValueDto> geographicDistribution() {
        return service.geographicDistribution();
    }

    @GetMapping
    public List<CustomerRowDto> customers(@RequestParam(required = false) String search,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false, defaultValue = "revenue") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        return service.customers(search, segment, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public CustomerDetailDto detail(@PathVariable long id) {
        return service.customerDetail(id);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
