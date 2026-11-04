package com.nexora.platform.analytics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.nexora.platform.databricks.DatabricksSqlClient;

/**
 * Customer Analytics (/analytics/customers). Reuses {@link DatabricksSqlClient}
 * against the same Gold-layer tables as {@link AnalyticsService}; this is a
 * distinct read path (per-customer aggregation) rather than a duplicate of it.
 *
 * Segmentation and "lifetime value" are both derived only from what the sample
 * data actually contains (orders within the single available Order_Date):
 *  - High-value: lifetime revenue in the top quartile of customers with orders.
 *  - Frequent: order count at or above the median for customers with orders.
 *  - New: exactly one order.
 *  - Inactive: a registered customer with zero orders.
 * "Customer lifetime value" here means lifetime revenue within this dataset,
 * not a projected/discounted CLV model — the UI labels it accordingly.
 */
@Service
public class CustomerAnalyticsService {

    private static final String PER_CUSTOMER_SQL = """
            SELECT c.Customer_ID, c.Company_Name, c.Email_Address,
                   COALESCE(a.State_Province, 'Unassigned') AS territory,
                   COUNT(DISTINCT soh.Sales_Order_ID) AS order_count,
                   COALESCE(SUM(soh.Total_Due), 0) AS lifetime_revenue
            FROM workspace.gold.customer c
            LEFT JOIN workspace.gold.salesorderheader soh ON soh.Customer_ID = c.Customer_ID
            LEFT JOIN workspace.gold.customeraddress ca ON ca.Customer_ID = c.Customer_ID
            LEFT JOIN workspace.gold.address a ON a.Address_ID = ca.Address_ID
            GROUP BY c.Customer_ID, c.Company_Name, c.Email_Address, a.State_Province
            """;

    private final DatabricksSqlClient sqlClient;

    public CustomerAnalyticsService(DatabricksSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    public CustomerSummaryDto summary() {
        List<CustomerRowDto> customers = allCustomers();
        long total = customers.size();
        long withOrders = customers.stream().filter(c -> c.orderCount() > 0).count();
        long returning = customers.stream().filter(c -> c.orderCount() > 1).count();
        long inactive = total - withOrders;
        double avgOrderValue = customers.stream().filter(c -> c.orderCount() > 0)
                .mapToDouble(CustomerRowDto::avgOrderValue).average().orElse(0);
        double avgOrders = withOrders == 0 ? 0 : customers.stream().mapToLong(CustomerRowDto::orderCount).sum() / (double) withOrders;
        return new CustomerSummaryDto(total, withOrders, returning, inactive, round2(avgOrderValue), round2(avgOrders));
    }

    public List<LabelValueDto> geographicDistribution() {
        List<LabelValueDto> result = new ArrayList<>();
        for (CustomerRowDto c : allCustomers()) {
            if (c.orderCount() == 0) continue;
            result.add(new LabelValueDto(c.territory(), c.lifetimeRevenue()));
        }
        return merge(result);
    }

    public List<CustomerRowDto> customers(String search, String segment, String sortBy, String sortDir) {
        List<CustomerRowDto> all = allCustomers();
        List<CustomerRowDto> filtered = new ArrayList<>(all);
        if (search != null && !search.isBlank()) {
            String needle = search.trim().toLowerCase();
            filtered = filtered.stream().filter(c -> c.companyName() != null && c.companyName().toLowerCase().contains(needle)).toList();
        }
        if (segment != null && !segment.isBlank() && !"all".equalsIgnoreCase(segment)) {
            filtered = filtered.stream().filter(c -> c.segment().equalsIgnoreCase(segment)).toList();
        }
        Comparator<CustomerRowDto> comparator = switch (sortBy == null ? "revenue" : sortBy) {
            case "name" -> Comparator.comparing(CustomerRowDto::companyName, Comparator.nullsLast(String::compareTo));
            case "orders" -> Comparator.comparingLong(CustomerRowDto::orderCount);
            default -> Comparator.comparingDouble(CustomerRowDto::lifetimeRevenue);
        };
        if (!"asc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }
        return filtered.stream().sorted(comparator).toList();
    }

    public CustomerDetailDto customerDetail(long customerId) {
        CustomerRowDto row = allCustomers().stream().filter(c -> c.customerId() == customerId).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No customer with id " + customerId));
        List<OrderRowDto> orders = new ArrayList<>();
        String sql = "SELECT soh.Sales_Order_Number, soh.Order_Date, soh.Total_Due, COUNT(sod.Sales_Order_Detail_ID) "
                + "FROM workspace.gold.salesorderheader soh "
                + "JOIN workspace.gold.salesorderdetail sod ON sod.Sales_Order_ID = soh.Sales_Order_ID "
                + "WHERE soh.Customer_ID = " + customerId
                + " GROUP BY soh.Sales_Order_Number, soh.Order_Date, soh.Total_Due ORDER BY soh.Total_Due DESC";
        for (List<String> r : sqlClient.executeQuery(sql)) {
            orders.add(new OrderRowDto(strAt(r, 0), strAt(r, 1), row.companyName(), doubleAt(r, 2), longAt(r, 3)));
        }
        return new CustomerDetailDto(row.customerId(), row.companyName(), null, row.territory(), row.orderCount(),
                row.lifetimeRevenue(), row.avgOrderValue(), row.segment(), orders);
    }

    private List<CustomerRowDto> allCustomers() {
        List<List<String>> rows = sqlClient.executeQuery(PER_CUSTOMER_SQL);
        List<long[]> orderCounts = new ArrayList<>();
        List<CustomerRowDto> raw = new ArrayList<>();
        for (List<String> r : rows) {
            long id = longAt(r, 0);
            long orderCount = longAt(r, 4);
            orderCounts.add(new long[] { orderCount });
            raw.add(new CustomerRowDto(id, strAt(r, 1), strAt(r, 3), orderCount, doubleAt(r, 5),
                    orderCount == 0 ? 0 : round2(doubleAt(r, 5) / orderCount), null));
        }
        double[] revenues = raw.stream().filter(c -> c.orderCount() > 0).mapToDouble(CustomerRowDto::lifetimeRevenue).sorted().toArray();
        double highValueThreshold = percentile(revenues, 0.75);
        long[] counts = raw.stream().filter(c -> c.orderCount() > 0).mapToLong(CustomerRowDto::orderCount).sorted().toArray();
        double medianOrders = percentileLong(counts, 0.5);

        List<CustomerRowDto> result = new ArrayList<>();
        for (CustomerRowDto c : raw) {
            String segment;
            if (c.orderCount() == 0) {
                segment = "Inactive";
            } else if (c.lifetimeRevenue() >= highValueThreshold && highValueThreshold > 0) {
                segment = "High-value";
            } else if (c.orderCount() == 1) {
                segment = "New";
            } else if (c.orderCount() >= medianOrders) {
                segment = "Frequent";
            } else {
                segment = "Standard";
            }
            result.add(new CustomerRowDto(c.customerId(), c.companyName(), c.territory(), c.orderCount(),
                    round2(c.lifetimeRevenue()), c.avgOrderValue(), segment));
        }
        return result;
    }

    private static List<LabelValueDto> merge(List<LabelValueDto> values) {
        List<LabelValueDto> result = new ArrayList<>();
        values.stream().collect(java.util.stream.Collectors.groupingBy(LabelValueDto::label,
                java.util.stream.Collectors.summingDouble(LabelValueDto::value)))
                .forEach((k, v) -> result.add(new LabelValueDto(k, round2(v))));
        result.sort((a, b) -> Double.compare(b.value(), a.value()));
        return result;
    }

    private static double percentile(double[] sorted, double p) {
        if (sorted.length == 0) return 0;
        int idx = Math.min(sorted.length - 1, (int) Math.ceil(p * sorted.length) - 1);
        return sorted[Math.max(0, idx)];
    }

    private static double percentileLong(long[] sorted, double p) {
        if (sorted.length == 0) return 0;
        int idx = Math.min(sorted.length - 1, (int) Math.ceil(p * sorted.length) - 1);
        return sorted[Math.max(0, idx)];
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
