package com.nexora.platform.sales;

import java.util.List;

public record FilterOptionsDto(List<String> categories, List<String> territories, String minOrderDate, String maxOrderDate) {
}
