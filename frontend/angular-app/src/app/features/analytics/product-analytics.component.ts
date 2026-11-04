import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

import { PanelComponent } from '../../shared/components/panel/panel.component';
import { HorizontalBarChartComponent, BarDatum } from '../../shared/components/charts/horizontal-bar-chart.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { AnalyticsApiService, ProductPerformance } from '../../core/services/analytics-api.service';

@Component({
  selector: 'app-product-analytics',
  standalone: true,
  imports: [CommonModule, PanelComponent, HorizontalBarChartComponent, DataTableComponent],
  templateUrl: './product-analytics.component.html',
  styleUrl: './product-analytics.component.scss',
})
export class ProductAnalyticsComponent implements OnInit {
  readonly products = signal<ProductPerformance[]>([]);
  readonly errorMessage = signal<string | null>(null);

  readonly columns: TableColumn[] = [
    { key: 'name', label: 'Product' },
    { key: 'category', label: 'Category' },
    { key: 'unitsSold', label: 'Units Sold', align: 'right' },
    { key: 'revenueFormatted', label: 'Revenue', align: 'right' },
  ];

  constructor(private readonly analyticsApi: AnalyticsApiService) {}

  ngOnInit(): void {
    this.analyticsApi.products().subscribe({
      next: (products) => this.products.set(products),
      error: () => this.errorMessage.set('Could not load product performance from the Spring Boot API.'),
    });
  }

  get topProductsChart(): BarDatum[] {
    const top = this.products().slice(0, 8);
    const total = top.reduce((sum, p) => sum + p.revenue, 0);
    return top.map((p) => ({ label: p.name, value: total === 0 ? 0 : Math.round((p.revenue / total) * 1000) / 10 }));
  }

  get rows() {
    return this.products().map((p) => ({ ...p, revenueFormatted: `$${p.revenue.toLocaleString(undefined, { maximumFractionDigits: 2 })}` }));
  }
}
