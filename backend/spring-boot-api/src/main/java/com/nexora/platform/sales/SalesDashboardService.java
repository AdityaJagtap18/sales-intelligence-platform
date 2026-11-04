package com.nexora.platform.sales;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.nexora.platform.analytics.LabelValueDto;
import com.nexora.platform.databricks.DatabricksSqlClient;

/**
 * Filtered sales analytics for the Sales Dashboard (/overview/sales-dashboard).
 * Builds on the same Gold-layer tables and {@link DatabricksSqlClient} read path
 * as {@code AnalyticsService} (Executive Overview), adding date/category/territory
 * filtering and drill-down that the unfiltered Executive Overview endpoints don't
 * need. Territory = Address.State_Province via Customer -> CustomerAddress ->
 * Address, the closest real geographic dimension in AdventureWorksLT.
 */
@Service
public class SalesDashboardService {

    private static final String BASE_JOIN = """
            FROM workspace.gold.salesorderheader soh
            JOIN workspace.gold.customer c ON soh.Customer_ID = c.Customer_ID
            JOIN workspace.gold.salesorderdetail sod ON sod.Sales_Order_ID = soh.Sales_Order_ID
            JOIN workspace.gold.product p ON sod.Product_ID = p.Product_ID
            LEFT JOIN workspace.gold.productcategory pc ON p.Product_Category_ID = pc.Product_Category_ID
            LEFT JOIN workspace.gold.customeraddress ca ON ca.Customer_ID = c.Customer_ID
            LEFT JOIN workspace.gold.address a ON a.Address_ID = ca.Address_ID
            """;

    private final DatabricksSqlClient sqlClient;

    public SalesDashboardService(DatabricksSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    public SalesSummaryDto summary(SalesFilter filter) {
        String sql = "SELECT SUM(soh.Total_Due), COUNT(DISTINCT soh.Sales_Order_ID), COUNT(DISTINCT soh.Customer_ID) "
                + BASE_JOIN + where(filter);
        List<String> row = sqlClient.executeQuery(sql).get(0);
        double totalRevenue = doubleAt(row, 0);
        long totalOrders = longAt(row, 1);
        long totalCustomers = longAt(row, 2);
        double avg = totalOrders == 0 ? 0 : totalRevenue / totalOrders;

        Double growth = null;
        if (!filter.hasDateRange()) {
            growth = null;
        }

        String period = filter.hasDateRange()
                ? ((filter.from() != null ? filter.from() : "earliest") + " to " + (filter.to() != null ? filter.to() : "latest"))
                : "All available orders";

        return new SalesSummaryDto(round2(totalRevenue), totalOrders, totalCustomers, round2(avg), growth, period);
    }

    public List<LabelValueDto> revenueByCategory(SalesFilter filter) {
        String sql = "SELECT pc.Name, SUM(sod.Line_Total) " + BASE_JOIN + where(filter)
                + " GROUP BY pc.Name ORDER BY 2 DESC";
        return labelValues(sql);
    }

    public List<LabelValueDto> revenueByTerritory(SalesFilter filter) {
        String sql = "SELECT COALESCE(a.State_Province, 'Unassigned'), SUM(sod.Line_Total) " + BASE_JOIN + where(filter)
                + " GROUP BY COALESCE(a.State_Province, 'Unassigned') ORDER BY 2 DESC";
        return labelValues(sql);
    }

    public List<LabelValueDto> topProducts(SalesFilter filter) {
        String sql = "SELECT p.Name, SUM(sod.Line_Total) " + BASE_JOIN + where(filter)
                + " GROUP BY p.Name ORDER BY 2 DESC LIMIT 10";
        return labelValues(sql);
    }

    public List<LabelValueDto> topCustomers(SalesFilter filter) {
        String sql = "SELECT c.Company_Name, SUM(sod.Line_Total) " + BASE_JOIN + where(filter)
                + " GROUP BY c.Company_Name ORDER BY 2 DESC LIMIT 10";
        return labelValues(sql);
    }

    public List<LabelValueDto> orderStatusDistribution(SalesFilter filter) {
        String sql = "SELECT COALESCE(CAST(soh.Status AS STRING), 'Unknown'), COUNT(DISTINCT soh.Sales_Order_ID) "
                + BASE_JOIN + where(filter) + " GROUP BY COALESCE(CAST(soh.Status AS STRING), 'Unknown')";
        return labelValues(sql);
    }

    public List<SalesOrderDetailDto> orders(SalesFilter filter) {
        String sql = "SELECT soh.Sales_Order_Number, soh.Order_Date, c.Company_Name, "
                + "COALESCE(a.State_Province, 'Unassigned'), COALESCE(CAST(soh.Status AS STRING), 'Unknown'), "
                + "soh.Total_Due, COUNT(sod.Sales_Order_Detail_ID) "
                + BASE_JOIN + where(filter)
                + " GROUP BY soh.Sales_Order_Number, soh.Order_Date, c.Company_Name, a.State_Province, soh.Status, soh.Total_Due"
                + " ORDER BY soh.Total_Due DESC";
        List<SalesOrderDetailDto> result = new ArrayList<>();
        for (List<String> row : sqlClient.executeQuery(sql)) {
            result.add(new SalesOrderDetailDto(strAt(row, 0), strAt(row, 1), strAt(row, 2), strAt(row, 3),
                    strAt(row, 4), doubleAt(row, 5), longAt(row, 6)));
        }
        return result;
    }

    public FilterOptionsDto filterOptions() {
        List<String> categories = sqlClient.executeQuery(
                "SELECT DISTINCT Name FROM workspace.gold.productcategory ORDER BY Name")
                .stream().map(r -> strAt(r, 0)).toList();
        List<String> territories = sqlClient.executeQuery(
                "SELECT DISTINCT State_Province FROM workspace.gold.address WHERE State_Province IS NOT NULL ORDER BY State_Province")
                .stream().map(r -> strAt(r, 0)).toList();
        List<List<String>> dateRange = sqlClient.executeQuery(
                "SELECT MIN(Order_Date), MAX(Order_Date) FROM workspace.gold.salesorderheader");
        String min = dateRange.isEmpty() ? null : strAt(dateRange.get(0), 0);
        String max = dateRange.isEmpty() ? null : strAt(dateRange.get(0), 1);
        return new FilterOptionsDto(categories, territories, min, max);
    }

    private List<LabelValueDto> labelValues(String sql) {
        List<LabelValueDto> result = new ArrayList<>();
        for (List<String> row : sqlClient.executeQuery(sql)) {
            result.add(new LabelValueDto(strAt(row, 0), round2(doubleAt(row, 1))));
        }
        return result;
    }

    private String where(SalesFilter filter) {
        List<String> clauses = new ArrayList<>();
        if (filter.from() != null) {
            clauses.add("soh.Order_Date >= '" + filter.from() + "'");
        }
        if (filter.to() != null) {
            clauses.add("soh.Order_Date <= '" + filter.to() + "'");
        }
        if (filter.category() != null) {
            clauses.add("pc.Name = " + SqlSafe.literal(filter.category()));
        }
        if (filter.territory() != null) {
            clauses.add("a.State_Province = " + SqlSafe.literal(filter.territory()));
        }
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
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
