package com.nexora.platform.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nexora.platform.databricks.DatabricksSqlClient;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private DatabricksSqlClient sqlClient;

    private AnalyticsService service() {
        return new AnalyticsService(sqlClient);
    }

    @Test
    void salesSummaryComputesAverageAndTopCategoryShareFromGoldRows() {
        when(sqlClient.executeQuery(anyString()))
                .thenReturn(List.of(List.of("1000.0", "4", "2", "3")))
                .thenReturn(List.of(List.of("Bikes", "700.0"), List.of("Accessories", "300.0")));

        SalesSummaryDto summary = service().salesSummary();

        assertThat(summary.totalRevenue()).isEqualTo(1000.0);
        assertThat(summary.totalOrders()).isEqualTo(4);
        assertThat(summary.avgOrderValue()).isEqualTo(250.0);
        assertThat(summary.totalCustomers()).isEqualTo(2);
        assertThat(summary.totalProducts()).isEqualTo(3);
        assertThat(summary.topCategoryName()).isEqualTo("Bikes");
        assertThat(summary.topCategoryRevenue()).isEqualTo(700.0);
        assertThat(summary.topCategorySharePct()).isEqualTo(70.0);
    }

    @Test
    void salesSummaryHandlesZeroOrdersAndZeroRevenueWithoutDividingByZero() {
        when(sqlClient.executeQuery(anyString()))
                .thenReturn(List.of(List.of("0.0", "0", "0", "0")))
                .thenReturn(List.of());

        SalesSummaryDto summary = service().salesSummary();

        assertThat(summary.avgOrderValue()).isZero();
        assertThat(summary.topCategoryName()).isEqualTo("Unknown");
        assertThat(summary.topCategorySharePct()).isZero();
    }

    @Test
    void revenueByCategoryConvertsRawRevenueToPercentageShareOfTotal() {
        when(sqlClient.executeQuery(anyString())).thenReturn(
                List.of(List.of("Bikes", "60.0"), List.of("Accessories", "40.0")));

        List<LabelValueDto> shares = service().revenueByCategory();

        assertThat(shares).containsExactly(
                new LabelValueDto("Bikes", 60.0),
                new LabelValueDto("Accessories", 40.0));
    }

    @Test
    void revenueTrendFlagsPointsBeyondTheZScoreThresholdAsAnomalies() {
        // 9 values clustered near 100, one extreme outlier at 1000.
        when(sqlClient.executeQuery(anyString())).thenReturn(List.of(
                List.of("SO1", "100.0"), List.of("SO2", "100.0"), List.of("SO3", "100.0"),
                List.of("SO4", "100.0"), List.of("SO5", "100.0"), List.of("SO6", "100.0"),
                List.of("SO7", "100.0"), List.of("SO8", "100.0"), List.of("SO9", "1000.0")));

        List<RevenueTrendPointDto> points = service().revenueTrend();

        assertThat(points).hasSize(9);
        assertThat(points.get(8).anomaly()).isTrue();
        assertThat(points.subList(0, 8)).allMatch(p -> !p.anomaly());
    }

    @Test
    void revenueTrendTreatsIdenticalValuesAsNoAnomalyRatherThanDividingByZeroStdDev() {
        when(sqlClient.executeQuery(anyString())).thenReturn(List.of(
                List.of("SO1", "500.0"), List.of("SO2", "500.0"), List.of("SO3", "500.0")));

        List<RevenueTrendPointDto> points = service().revenueTrend();

        assertThat(points).allMatch(p -> !p.anomaly());
    }

    @Test
    void anomaliesBucketsImpactByZScoreMagnitudeAndSortsByConfidenceDescending() {
        // A 15-row cluster at 100 plus two outliers (300, 320) close enough
        // together that neither alone dominates the mean/stdDev the way a
        // single huge outlier would — both should clear the threshold.
        List<List<String>> rows = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) {
            rows.add(List.of("SO" + i, "100.0"));
        }
        rows.add(List.of("SO15", "300.0"));
        rows.add(List.of("SO16", "320.0"));
        when(sqlClient.executeQuery(anyString())).thenReturn(rows);

        List<AnomalyRowDto> anomalies = service().anomalies();

        assertThat(anomalies).hasSize(2);
        assertThat(anomalies).allMatch(a -> a.metric().equals("Order Value") && a.direction().equals("up"));
        assertThat(anomalies.get(0).date()).isEqualTo("SO16");
        assertThat(anomalies.get(0).confidence()).isGreaterThanOrEqualTo(anomalies.get(1).confidence());
    }

    @Test
    void anomaliesCapsConfidenceAt99EvenForExtremeZScores() {
        // A single outlier's own z-score is bounded by sqrt(n-1) because the
        // population stdDev includes the outlier itself — a large n and a
        // wildly disproportionate outlier is needed to push past the
        // confidence*35 >= 99 cutoff and exercise the cap.
        List<List<String>> rows = new java.util.ArrayList<>();
        for (int i = 0; i < 19; i++) {
            rows.add(List.of("SO" + i, "1.0"));
        }
        rows.add(List.of("SO19", "1000000.0"));
        when(sqlClient.executeQuery(anyString())).thenReturn(rows);

        List<AnomalyRowDto> anomalies = service().anomalies();

        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.get(0).confidence()).isEqualTo(99);
        assertThat(anomalies.get(0).impact()).isEqualTo("High");
    }
}
