package com.nexora.platform.forecasting;

public record ForecastPointDto(String label, Double actual, Double predicted, Double lowerBound, Double upperBound) {
}
