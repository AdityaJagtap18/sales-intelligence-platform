package com.nexora.platform.analytics;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.nexora.platform.databricks.DatabricksSqlClient;

/**
 * Business Intelligence over workspace.gold.* via the existing
 * {@link DatabricksSqlClient} — the same read path Phases 5-7 already use,
 * not a new JDBC connection or SQL Warehouse client.
 */
@Service
public class AnalyticsService {

    private static final String SUMMARY_SQL = """
            SELECT
              (SELECT SUM(Total_Due) FROM workspace.gold.salesorderheader) AS total_revenue,
              (SELECT COUNT(*) FROM workspace.gold.salesorderheader) AS total_orders,
              (SELECT COUNT(*) FROM workspace.gold.customer) AS total_customers,
              (SELECT COUNT(*) FROM workspace.gold.product) AS total_products
            """;

    private static final String REVENUE_BY_CATEGORY_SQL = """
            SELECT pc.Name AS category, SUM(sod.Line_Total) AS revenue
            FROM workspace.gold.salesorderdetail sod
            JOIN workspace.gold.product p ON sod.Product_ID = p.Product_ID
            JOIN workspace.gold.productcategory pc ON p.Product_Category_ID = pc.Product_Category_ID
            GROUP BY pc.Name
            ORDER BY revenue DESC
            """;

    private static final String REVENUE_BY_CUSTOMER_SQL = """
            SELECT c.Company_Name AS customer, SUM(soh.Total_Due) AS revenue
            FROM workspace.gold.salesorderheader soh
            JOIN workspace.gold.customer c ON soh.Customer_ID = c.Customer_ID
            GROUP BY c.Company_Name
            ORDER BY revenue DESC
            LIMIT 5
            """;

    // AdventureWorksLT's sample order data is all a single Order_Date, so a
    // day-over-day trend has one point. Per-order value has real variance
    // and chronological order (Sales_Order_ID is assigned in order placed),
    // so that's the basis for the trend chart and the anomaly check instead.
    private static final String ORDER_VALUES_SQL = """
            SELECT Sales_Order_Number, Total_Due
            FROM workspace.gold.salesorderheader
            ORDER BY Sales_Order_ID
            """;

    private static final String ORDERS_SQL = """
            SELECT soh.Sales_Order_Number, soh.Order_Date, c.Company_Name, soh.Total_Due, COUNT(sod.Sales_Order_Detail_ID)
            FROM workspace.gold.salesorderheader soh
            JOIN workspace.gold.customer c ON soh.Customer_ID = c.Customer_ID
            JOIN workspace.gold.salesorderdetail sod ON sod.Sales_Order_ID = soh.Sales_Order_ID
            GROUP BY soh.Sales_Order_Number, soh.Order_Date, c.Company_Name, soh.Total_Due
            ORDER BY soh.Total_Due DESC
            """;

    private static final String PRODUCT_PERFORMANCE_SQL = """
            SELECT p.Name, pc.Name AS category, SUM(sod.Order_Qty), SUM(sod.Line_Total)
            FROM workspace.gold.salesorderdetail sod
            JOIN workspace.gold.product p ON sod.Product_ID = p.Product_ID
            LEFT JOIN workspace.gold.productcategory pc ON p.Product_Category_ID = pc.Product_Category_ID
            GROUP BY p.Name, pc.Name
            ORDER BY SUM(sod.Line_Total) DESC
            LIMIT 20
            """;

    /** |z-score| beyond this on a day's revenue is flagged as an anomaly. */
    private static final double ANOMALY_Z_THRESHOLD = 1.3;

    private final DatabricksSqlClient sqlClient;

    public AnalyticsService(DatabricksSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    public SalesSummaryDto salesSummary() {
        List<String> row = sqlClient.executeQuery(SUMMARY_SQL).get(0);
        double totalRevenue = doubleAt(row, 0);
        long totalOrders = longAt(row, 1);
        long totalCustomers = longAt(row, 2);
        long totalProducts = longAt(row, 3);
        double avgOrderValue = totalOrders == 0 ? 0 : totalRevenue / totalOrders;

        List<List<String>> categoryRows = sqlClient.executeQuery(REVENUE_BY_CATEGORY_SQL);
        String topCategoryName = "Unknown";
        double topCategoryRevenue = 0;
        if (!categoryRows.isEmpty()) {
            topCategoryName = strAt(categoryRows.get(0), 0);
            topCategoryRevenue = doubleAt(categoryRows.get(0), 1);
        }
        double topShare = totalRevenue == 0 ? 0 : (topCategoryRevenue / totalRevenue) * 100;

        return new SalesSummaryDto(totalRevenue, totalOrders, avgOrderValue, totalCustomers, totalProducts,
                topCategoryName, round2(topCategoryRevenue), round2(topShare));
    }

    /** Revenue share per category, as a percentage of total category revenue (fits the bar chart's "%" display). */
    public List<LabelValueDto> revenueByCategory() {
        List<List<String>> rows = sqlClient.executeQuery(REVENUE_BY_CATEGORY_SQL);
        double total = rows.stream().mapToDouble(r -> doubleAt(r, 1)).sum();
        List<LabelValueDto> result = new ArrayList<>();
        for (List<String> row : rows) {
            double revenue = doubleAt(row, 1);
            double sharePct = total == 0 ? 0 : (revenue / total) * 100;
            result.add(new LabelValueDto(strAt(row, 0), round2(sharePct)));
        }
        return result;
    }

    public List<LabelValueDto> revenueByTopCustomers() {
        List<LabelValueDto> result = new ArrayList<>();
        for (List<String> row : sqlClient.executeQuery(REVENUE_BY_CUSTOMER_SQL)) {
            result.add(new LabelValueDto(strAt(row, 0), round2(doubleAt(row, 1))));
        }
        return result;
    }

    public List<RevenueTrendPointDto> revenueTrend() {
        List<List<String>> rows = sqlClient.executeQuery(ORDER_VALUES_SQL);
        double[] values = rows.stream().mapToDouble(r -> doubleAt(r, 1)).toArray();
        double mean = mean(values);
        double stdDev = stdDev(values, mean);

        List<RevenueTrendPointDto> points = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            double value = values[i];
            double z = stdDev == 0 ? 0 : (value - mean) / stdDev;
            boolean anomaly = Math.abs(z) > ANOMALY_Z_THRESHOLD;
            points.add(new RevenueTrendPointDto(strAt(row, 0), round2(value), round2(mean), anomaly));
        }
        return points;
    }

    /** Derived from the same z-score pass as {@link #revenueTrend()} — the Phase 9 "simple statistical method". */
    public List<AnomalyRowDto> anomalies() {
        List<List<String>> rows = sqlClient.executeQuery(ORDER_VALUES_SQL);
        double[] values = rows.stream().mapToDouble(r -> doubleAt(r, 1)).toArray();
        double mean = mean(values);
        double stdDev = stdDev(values, mean);

        List<AnomalyRowDto> anomalies = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            double value = values[i];
            double z = stdDev == 0 ? 0 : (value - mean) / stdDev;
            if (Math.abs(z) <= ANOMALY_Z_THRESHOLD) {
                continue;
            }
            String impact = Math.abs(z) > 2.0 ? "High" : Math.abs(z) > 1.6 ? "Medium" : "Low";
            int confidence = (int) Math.min(99, Math.round(Math.abs(z) * 35));
            anomalies.add(new AnomalyRowDto("Order Value", strAt(rows.get(i), 0), impact,
                    z > 0 ? "up" : "down", confidence));
        }
        anomalies.sort((a, b) -> Integer.compare(b.confidence(), a.confidence()));
        return anomalies;
    }

    public List<OrderRowDto> orders() {
        List<OrderRowDto> result = new ArrayList<>();
        for (List<String> row : sqlClient.executeQuery(ORDERS_SQL)) {
            result.add(new OrderRowDto(strAt(row, 0), strAt(row, 1), strAt(row, 2), doubleAt(row, 3), longAt(row, 4)));
        }
        return result;
    }

    public List<ProductPerformanceDto> productPerformance() {
        List<ProductPerformanceDto> result = new ArrayList<>();
        for (List<String> row : sqlClient.executeQuery(PRODUCT_PERFORMANCE_SQL)) {
            result.add(new ProductPerformanceDto(strAt(row, 0), row.get(1), longAt(row, 2), doubleAt(row, 3)));
        }
        return result;
    }

    private static double mean(double[] values) {
        if (values.length == 0) return 0;
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    private static double stdDev(double[] values, double mean) {
        if (values.length == 0) return 0;
        double sumSq = 0;
        for (double v : values) sumSq += (v - mean) * (v - mean);
        return Math.sqrt(sumSq / values.length);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String strAt(List<String> row, int i) {
        return row.get(i);
    }

    private static long longAt(List<String> row, int i) {
        String v = row.get(i);
        return v == null ? 0 : Long.parseLong(v);
    }

    private static double doubleAt(List<String> row, int i) {
        String v = row.get(i);
        return v == null ? 0 : Double.parseDouble(v);
    }
}
