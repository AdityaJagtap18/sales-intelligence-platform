package com.nexora.platform.sales;

/**
 * Filter values come from UI dropdowns populated by {@code /api/sales/filter-options}
 * (real distinct values from the Gold layer), but are still untrusted input on the
 * way back in. {@link DatabricksSqlClient#executeQuery} takes a raw SQL string with
 * no parameter binding, so filter values are validated to a safe character set and
 * quote-escaped before being inlined, rather than ever being executed as-is.
 */
final class SqlSafe {

    private SqlSafe() {
    }

    static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Validates against a safe character set, then escapes single quotes for a SQL string literal. */
    static String literal(String value) {
        if (!value.matches("[A-Za-z0-9 .,&'/()-]{1,128}")) {
            throw new IllegalArgumentException("Invalid filter value");
        }
        return "'" + value.replace("'", "''") + "'";
    }
}
