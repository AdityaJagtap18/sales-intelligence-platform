package com.nexora.platform.sales;

import java.time.LocalDate;

/**
 * Sales Dashboard filter set. {@code territory} maps to Address.State_Province
 * in the Gold layer — AdventureWorksLT has no dedicated SalesTerritory table,
 * so state/province is the closest real dimension and is labeled "Territory"
 * in the UI rather than inventing a field that doesn't exist.
 */
public record SalesFilter(LocalDate from, LocalDate to, String category, String territory) {

    public static SalesFilter of(String from, String to, String category, String territory) {
        return new SalesFilter(
                parseDate(from),
                parseDate(to),
                SqlSafe.normalize(category),
                SqlSafe.normalize(territory));
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    public boolean hasDateRange() {
        return from != null || to != null;
    }
}
