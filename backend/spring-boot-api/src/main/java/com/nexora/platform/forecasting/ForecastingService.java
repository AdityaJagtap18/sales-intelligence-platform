package com.nexora.platform.forecasting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.platform.databricks.DatabricksSqlClient;

/**
 * Real, minimal forecasting over Gold-layer order data: linear regression on
 * per-calendar-period aggregates. Requires at least {@link #MIN_PERIODS}
 * distinct Order_Date values to produce a forecast — AdventureWorksLT's
 * sample normally has only one, so most runs will honestly report
 * INSUFFICIENT_DATA rather than return an arbitrary number, per the spec's
 * "never present arbitrary generated values as real forecasts" requirement.
 * No ML/forecasting library is in the backend's pom.xml and no Python
 * worker service runs at request time (backend/python-worker is a batch
 * ingestion script, not a callable service) — linear regression needs
 * neither and is implemented directly here, in Spring Boot, as the spec's
 * fallback ("use an appropriate forecasting method based on data
 * availability") rather than adding new infrastructure for one feature.
 */
@Service
public class ForecastingService {

    private static final int MIN_PERIODS = 3;

    private static final String REVENUE_BY_PERIOD_SQL = """
            SELECT CAST(Order_Date AS STRING), SUM(Total_Due)
            FROM workspace.gold.salesorderheader
            GROUP BY Order_Date
            ORDER BY Order_Date
            """;

    private static final String ORDERS_BY_PERIOD_SQL = """
            SELECT CAST(Order_Date AS STRING), COUNT(*)
            FROM workspace.gold.salesorderheader
            GROUP BY Order_Date
            ORDER BY Order_Date
            """;

    private final DatabricksSqlClient sqlClient;
    private final ForecastRunRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ForecastingService(DatabricksSqlClient sqlClient, ForecastRunRepository repository) {
        this.sqlClient = sqlClient;
        this.repository = repository;
    }

    public ForecastRunDto generate(String metric, int horizon, String requestedBy) {
        boolean revenue = "REVENUE".equalsIgnoreCase(metric);
        List<List<String>> rows = sqlClient.executeQuery(revenue ? REVENUE_BY_PERIOD_SQL : ORDERS_BY_PERIOD_SQL);

        if (rows.size() < MIN_PERIODS) {
            String reason = "Only " + rows.size() + " distinct Order_Date value(s) exist in workspace.gold.salesorderheader. "
                    + "At least " + MIN_PERIODS + " distinct calendar periods are required to fit a trend; "
                    + "the AdventureWorksLT sample dataset has a single order date, so period-over-period "
                    + "forecasting cannot be performed honestly against it.";
            ForecastRun run = new ForecastRun(metric.toUpperCase(), horizon, "INSUFFICIENT_DATA", null, null, null,
                    reason, null, requestedBy, Instant.now());
            repository.save(run);
            return ForecastRunDto.insufficientData(metric.toUpperCase(), horizon, reason);
        }

        double[] x = new double[rows.size()];
        double[] y = new double[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            x[i] = i;
            y[i] = Double.parseDouble(rows.get(i).get(1));
        }
        double[] coeffs = linearRegression(x, y);
        double slope = coeffs[0];
        double intercept = coeffs[1];

        List<ForecastPointDto> historical = new ArrayList<>();
        double sumAbsErr = 0;
        double sumPctErr = 0;
        for (int i = 0; i < rows.size(); i++) {
            double fitted = slope * x[i] + intercept;
            sumAbsErr += Math.abs(y[i] - fitted);
            if (y[i] != 0) sumPctErr += Math.abs((y[i] - fitted) / y[i]);
            historical.add(new ForecastPointDto(rows.get(i).get(0), round2(y[i]), round2(fitted), null, null));
        }
        double mae = round2(sumAbsErr / rows.size());
        double mape = round2((sumPctErr / rows.size()) * 100);
        double residualStdDev = Math.sqrt(historical.stream().mapToDouble(p -> Math.pow(p.actual() - p.predicted(), 2)).sum() / rows.size());

        List<ForecastPointDto> predicted = new ArrayList<>();
        for (int i = 0; i < horizon; i++) {
            double xi = rows.size() + i;
            double value = slope * xi + intercept;
            predicted.add(new ForecastPointDto("Period +" + (i + 1), null, round2(Math.max(0, value)),
                    round2(Math.max(0, value - 1.96 * residualStdDev)), round2(value + 1.96 * residualStdDev)));
        }

        ForecastRunDto dto = new ForecastRunDto(null, metric.toUpperCase(), horizon, "COMPLETED", "Linear regression (least squares)",
                mae, mape, null, historical, predicted, null);
        persist(dto, requestedBy);
        return dto;
    }

    public List<ForecastRunDto> history() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    private void persist(ForecastRunDto dto, String requestedBy) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            ForecastRun run = new ForecastRun(dto.metric(), dto.horizonPeriods(), dto.status(), dto.method(), dto.mae(),
                    dto.mape(), dto.reason(), json, requestedBy, Instant.now());
            repository.save(run);
        } catch (Exception ignored) {
            // Persisting history is best-effort; the generated forecast is still returned to the caller.
        }
    }

    private ForecastRunDto toDto(ForecastRun run) {
        List<ForecastPointDto> historical = List.of();
        List<ForecastPointDto> predicted = List.of();
        if (run.getResultJson() != null) {
            try {
                ForecastRunDto parsed = objectMapper.readValue(run.getResultJson(), ForecastRunDto.class);
                historical = parsed.historical();
                predicted = parsed.predicted();
            } catch (Exception ignored) {
                // Fall back to summary-only fields below if the stored JSON can't be parsed.
            }
        }
        return new ForecastRunDto(run.getId(), run.getMetric(), run.getHorizonPeriods(), run.getStatus(), run.getMethod(),
                run.getMae(), run.getMape(), run.getReason(), historical, predicted, run.getCreatedAt().toString());
    }

    private static double[] linearRegression(double[] x, double[] y) {
        int n = x.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumXX += x[i] * x[i];
        }
        double denom = n * sumXX - sumX * sumX;
        double slope = denom == 0 ? 0 : (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;
        return new double[] { slope, intercept };
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
