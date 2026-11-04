package com.nexora.platform.analytics;

/** Shared shape for the Angular bar/donut chart inputs ({@code BarDatum}/{@code DonutDatum}). */
public record LabelValueDto(String label, double value) {
}
