package com.nexora.platform.sales;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexora.platform.analytics.LabelValueDto;

@RestController
@RequestMapping("/sales")
public class SalesDashboardController {

    private final SalesDashboardService service;

    public SalesDashboardController(SalesDashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public SalesSummaryDto summary(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String category, @RequestParam(required = false) String territory) {
        return service.summary(SalesFilter.of(from, to, category, territory));
    }

    @GetMapping("/revenue-by-category")
    public List<LabelValueDto> revenueByCategory(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String category, @RequestParam(required = false) String territory) {
        return service.revenueByCategory(SalesFilter.of(from, to, category, territory));
    }

    @GetMapping("/revenue-by-territory")
    public List<LabelValueDto> revenueByTerritory(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String category, @RequestParam(required = false) String territory) {
        return service.revenueByTerritory(SalesFilter.of(from, to, category, territory));
    }

    @GetMapping("/top-products")
    public List<LabelValueDto> topProducts(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String category, @RequestParam(required = false) String territory) {
        return service.topProducts(SalesFilter.of(from, to, category, territory));
    }

    @GetMapping("/top-customers")
    public List<LabelValueDto> topCustomers(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String category, @RequestParam(required = false) String territory) {
        return service.topCustomers(SalesFilter.of(from, to, category, territory));
    }

    @GetMapping("/order-status")
    public List<LabelValueDto> orderStatus(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String category, @RequestParam(required = false) String territory) {
        return service.orderStatusDistribution(SalesFilter.of(from, to, category, territory));
    }

    @GetMapping("/orders")
    public List<SalesOrderDetailDto> orders(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String category, @RequestParam(required = false) String territory) {
        return service.orders(SalesFilter.of(from, to, category, territory));
    }

    @GetMapping("/filter-options")
    public FilterOptionsDto filterOptions() {
        return service.filterOptions();
    }

    @GetMapping(value = "/orders/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportOrders(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String category, @RequestParam(required = false) String territory) {
        List<SalesOrderDetailDto> orders = service.orders(SalesFilter.of(from, to, category, territory));
        StringBuilder csv = new StringBuilder("Order Number,Order Date,Customer,Territory,Status,Total Due,Line Items\n");
        for (SalesOrderDetailDto o : orders) {
            csv.append(csvField(o.orderNumber())).append(',')
                    .append(csvField(o.orderDate())).append(',')
                    .append(csvField(o.customer())).append(',')
                    .append(csvField(o.territory())).append(',')
                    .append(csvField(o.status())).append(',')
                    .append(o.totalDue()).append(',')
                    .append(o.lineItemCount()).append('\n');
        }
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("sales-orders.csv").build().toString())
                .body(body);
    }

    private static String csvField(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
