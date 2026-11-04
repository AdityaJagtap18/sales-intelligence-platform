package com.nexora.platform.forecasting;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics/forecasting")
public class ForecastingController {

    private final ForecastingService service;

    public ForecastingController(ForecastingService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public ForecastRunDto generate(@RequestParam(defaultValue = "REVENUE") String metric,
            @RequestParam(defaultValue = "4") int horizon) {
        int boundedHorizon = Math.max(1, Math.min(12, horizon));
        return service.generate(metric, boundedHorizon, "angular-ui");
    }

    @GetMapping("/history")
    public List<ForecastRunDto> history() {
        return service.history();
    }
}
