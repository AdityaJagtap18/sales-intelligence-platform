package com.nexora.platform.forecasting;

import java.util.List;

public record ForecastRunDto(
        Long id,
        String metric,
        int horizonPeriods,
        String status,
        String method,
        Double mae,
        Double mape,
        String reason,
        List<ForecastPointDto> historical,
        List<ForecastPointDto> predicted,
        String createdAt) {

    public static ForecastRunDto insufficientData(String metric, int horizon, String reason) {
        return new ForecastRunDto(null, metric, horizon, "INSUFFICIENT_DATA", null, null, null, reason, List.of(), List.of(), null);
    }
}
