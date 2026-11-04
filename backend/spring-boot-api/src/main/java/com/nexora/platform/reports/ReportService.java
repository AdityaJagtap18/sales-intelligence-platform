package com.nexora.platform.reports;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.nexora.platform.analytics.AnalyticsService;
import com.nexora.platform.analytics.CustomerAnalyticsService;
import com.nexora.platform.analytics.CustomerRowDto;
import com.nexora.platform.analytics.ProductPerformanceDto;
import com.nexora.platform.databricks.DatabricksSqlClient;
import com.nexora.platform.pipeline.PipelineRunDto;
import com.nexora.platform.pipeline.PipelineRunService;
import com.nexora.platform.sales.SalesDashboardService;
import com.nexora.platform.sales.SalesFilter;
import com.nexora.platform.sales.SalesOrderDetailDto;

/**
 * Report management (/overview/reports). Each report composes existing
 * services (AnalyticsService, CustomerAnalyticsService, SalesDashboardService,
 * PipelineRunService) rather than re-querying Databricks independently, and
 * every generation is persisted to {@code report_execution} so the history
 * list reflects real runs. Reports with no backing data source yet (none
 * currently) would be omitted from {@link #definitions()} rather than shown
 * with a disabled "Generate" button, per the spec's "no actions with no
 * implementation" rule.
 */
@Service
public class ReportService {

    private static final List<ReportDefinitionDto> DEFINITIONS = List.of(
            new ReportDefinitionDto("SALES_PERFORMANCE", "Sales Performance", "Sales",
                    "Orders with revenue, territory and status, from the Sales Dashboard data path."),
            new ReportDefinitionDto("CUSTOMER", "Customer Summary", "Customer",
                    "Per-customer order count, lifetime revenue and segment."),
            new ReportDefinitionDto("PRODUCT", "Product Performance", "Product",
                    "Units sold and revenue per product, reusing AnalyticsService#productPerformance()."),
            new ReportDefinitionDto("REGIONAL", "Regional Revenue", "Regional",
                    "Revenue by territory (Address.State_Province)."),
            new ReportDefinitionDto("DATA_QUALITY", "Data Quality (latest run)", "Data Quality",
                    "Latest workspace.nexora.data_quality_report run, the same source the Phase 5 quality gauges use."),
            new ReportDefinitionDto("PIPELINE_EXECUTION", "Pipeline Execution History", "Pipeline",
                    "Databricks job run history from PipelineRunService / pipeline_run."));

    private final AnalyticsService analyticsService;
    private final CustomerAnalyticsService customerAnalyticsService;
    private final SalesDashboardService salesDashboardService;
    private final PipelineRunService pipelineRunService;
    private final DatabricksSqlClient sqlClient;
    private final ReportExecutionRepository repository;

    public ReportService(AnalyticsService analyticsService, CustomerAnalyticsService customerAnalyticsService,
            SalesDashboardService salesDashboardService, PipelineRunService pipelineRunService,
            DatabricksSqlClient sqlClient, ReportExecutionRepository repository) {
        this.analyticsService = analyticsService;
        this.customerAnalyticsService = customerAnalyticsService;
        this.salesDashboardService = salesDashboardService;
        this.pipelineRunService = pipelineRunService;
        this.sqlClient = sqlClient;
        this.repository = repository;
    }

    public List<ReportDefinitionDto> definitions() {
        return DEFINITIONS;
    }

    public ReportResultDto generate(String reportKey, String from, String to, String category, String territory) {
        ReportDefinitionDto def = DEFINITIONS.stream().filter(d -> d.key().equalsIgnoreCase(reportKey)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Unknown report: " + reportKey));

        List<String> columns;
        List<Map<String, Object>> rows;
        String status = "COMPLETED";
        String error = null;
        try {
            switch (def.key()) {
                case "SALES_PERFORMANCE" -> {
                    columns = List.of("Order Number", "Order Date", "Customer", "Territory", "Status", "Total Due", "Line Items");
                    SalesFilter filter = SalesFilter.of(from, to, category, territory);
                    rows = salesDashboardService.orders(filter).stream().map(this::toMap).toList();
                }
                case "CUSTOMER" -> {
                    columns = List.of("Customer ID", "Company", "Territory", "Order Count", "Lifetime Revenue", "Avg Order Value", "Segment");
                    rows = customerAnalyticsService.customers(null, null, "revenue", "desc").stream().map(this::toMap).toList();
                }
                case "PRODUCT" -> {
                    columns = List.of("Product", "Category", "Units Sold", "Revenue");
                    rows = analyticsService.productPerformance().stream().map(this::toMap).toList();
                }
                case "REGIONAL" -> {
                    columns = List.of("Territory", "Revenue");
                    List<Map<String, Object>> regional = new ArrayList<>();
                    for (var v : salesDashboardService.revenueByTerritory(SalesFilter.of(from, to, category, territory))) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("Territory", v.label());
                        m.put("Revenue", v.value());
                        regional.add(m);
                    }
                    rows = regional;
                }
                case "DATA_QUALITY" -> {
                    columns = List.of("Table", "Check", "Status");
                    rows = dataQualityRows();
                }
                case "PIPELINE_EXECUTION" -> {
                    columns = List.of("Run ID", "Lifecycle State", "Result State", "Started At", "Completed At");
                    rows = pipelineRunService.listRuns().stream().map(this::toMap).toList();
                }
                default -> throw new NoSuchElementException("Unknown report: " + reportKey);
            }
        } catch (NoSuchElementException e) {
            throw e;
        } catch (Exception e) {
            status = "FAILED";
            error = e.getMessage();
            columns = List.of();
            rows = List.of();
        }

        Instant now = Instant.now();
        ReportExecution execution = repository.save(new ReportExecution(def.key(), def.name(), null, status,
                rows.size(), error, "angular-ui", now));
        return new ReportResultDto(execution.getId(), def.key(), def.name(), status, columns, rows, now.toString(), error);
    }

    public List<ReportExecutionDto> history() {
        return repository.findAllByOrderByGeneratedAtDesc().stream()
                .map(e -> new ReportExecutionDto(e.getId(), e.getReportKey(), e.getReportName(), e.getStatus(),
                        e.getRowCount(), e.getErrorMessage(), e.getGeneratedAt().toString()))
                .toList();
    }

    private List<Map<String, Object>> dataQualityRows() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT table_name, check_name, status FROM workspace.nexora.data_quality_report "
                + "WHERE run_id = (SELECT max(run_id) FROM workspace.nexora.data_quality_report)";
        for (List<String> row : sqlClient.executeQuery(sql)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("Table", row.get(0));
            m.put("Check", row.get(1));
            m.put("Status", row.get(2));
            result.add(m);
        }
        return result;
    }

    private Map<String, Object> toMap(SalesOrderDetailDto o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("Order Number", o.orderNumber());
        m.put("Order Date", o.orderDate());
        m.put("Customer", o.customer());
        m.put("Territory", o.territory());
        m.put("Status", o.status());
        m.put("Total Due", o.totalDue());
        m.put("Line Items", o.lineItemCount());
        return m;
    }

    private Map<String, Object> toMap(CustomerRowDto c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("Customer ID", c.customerId());
        m.put("Company", c.companyName());
        m.put("Territory", c.territory());
        m.put("Order Count", c.orderCount());
        m.put("Lifetime Revenue", c.lifetimeRevenue());
        m.put("Avg Order Value", c.avgOrderValue());
        m.put("Segment", c.segment());
        return m;
    }

    private Map<String, Object> toMap(ProductPerformanceDto p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("Product", p.name());
        m.put("Category", p.category());
        m.put("Units Sold", p.unitsSold());
        m.put("Revenue", p.revenue());
        return m;
    }

    private Map<String, Object> toMap(PipelineRunDto p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("Run ID", p.id());
        m.put("Lifecycle State", p.lifecycleState());
        m.put("Result State", p.resultState());
        m.put("Started At", p.startedAt());
        m.put("Completed At", p.completedAt());
        return m;
    }
}
