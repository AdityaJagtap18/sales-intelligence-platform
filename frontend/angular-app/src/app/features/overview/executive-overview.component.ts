import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { KpiCardComponent } from '../../shared/components/kpi-card/kpi-card.component';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { FilterBarComponent } from '../../shared/components/filter-bar/filter-bar.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { AreaLineChartComponent, AreaLinePoint } from '../../shared/components/charts/area-line-chart.component';
import { HorizontalBarChartComponent, BarDatum } from '../../shared/components/charts/horizontal-bar-chart.component';
import { DonutChartComponent, DonutDatum } from '../../shared/components/charts/donut-chart.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { AnalyticsApiService, SalesSummary, AnomalyRow, DateRange } from '../../core/services/analytics-api.service';
import { SalesApiService } from '../../core/services/sales-api.service';
import { DateRangeChange } from '../../shared/components/filter-bar/filter-bar.component';

interface KpiView {
  label: string;
  value: string;
  icon: string;
  deltaLabel: string;
  sparkline: number[];
}

@Component({
  selector: 'app-executive-overview',
  standalone: true,
  imports: [
    CommonModule,
    KpiCardComponent,
    PanelComponent,
    BadgeComponent,
    FilterBarComponent,
    IconComponent,
    AreaLineChartComponent,
    HorizontalBarChartComponent,
    DonutChartComponent,
    DataTableComponent,
  ],
  templateUrl: './executive-overview.component.html',
  styleUrl: './executive-overview.component.scss',
})
export class ExecutiveOverviewComponent implements OnInit {
  readonly kpis = signal<KpiView[]>([]);
  readonly revenueSeries = signal<AreaLinePoint[]>([]);
  readonly revenueByCategory = signal<BarDatum[]>([]);
  readonly revenueByTopCustomers = signal<DonutDatum[]>([]);
  readonly recentAnomalies = signal<AnomalyRow[]>([]);
  readonly summary = signal<SalesSummary | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly minOrderDate = signal<string | null>(null);
  readonly maxOrderDate = signal<string | null>(null);
  readonly rangeLabel = signal('All available orders');
  private sparkline: number[] = [];
  private range: DateRange = {};

  readonly anomalyColumns: TableColumn[] = [
    { key: 'metric', label: 'Metric' },
    { key: 'date', label: 'Order' },
    { key: 'impact', label: 'Impact' },
    { key: 'confidence', label: 'Confidence', align: 'right' },
  ];

  constructor(private readonly analyticsApi: AnalyticsApiService, private readonly salesApi: SalesApiService) {}

  ngOnInit(): void {
    this.salesApi.filterOptions().subscribe({
      next: (o) => {
        this.minOrderDate.set(o.minOrderDate);
        this.maxOrderDate.set(o.maxOrderDate);
      },
    });

    this.loadData();

    // Order Value Trend and anomaly detection are based on order sequence
    // (Sales_Order_ID), not Order_Date, so they aren't affected by the date
    // filter — see AnalyticsService#revenueTrend().
    this.analyticsApi.revenueTrend().subscribe({
      next: (points) => {
        // 32 order numbers would overflow the chart's x-axis label row if
        // every one were shown (each is an unbreakable ~45px-wide token) —
        // thin them out the same way the Phase 2 mock data did.
        const step = Math.max(1, Math.ceil(points.length / 8));
        this.revenueSeries.set(
          points.map((p, i) => ({
            label: p.label,
            value: p.value,
            expected: p.expected,
            anomaly: p.anomaly,
            showLabel: i === 0 || i === points.length - 1 || i % step === 0,
          })),
        );
        this.sparkline = points.slice(-8).map((p) => p.value);
        this.buildKpis();
      },
      error: () => this.errorMessage.set('Could not load the revenue trend.'),
    });

    this.analyticsApi.anomalies().subscribe({
      next: (rows) => this.recentAnomalies.set(rows),
      error: () => this.errorMessage.set('Could not load anomalies.'),
    });
  }

  private loadData(): void {
    this.analyticsApi.salesSummary(this.range).subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.errorMessage.set(null);
        this.buildKpis();
      },
      error: () => this.errorMessage.set('Could not load the sales summary from the Spring Boot API.'),
    });

    this.analyticsApi.revenueByCategory(this.range).subscribe({
      next: (data) => this.revenueByCategory.set(data),
      error: () => this.errorMessage.set('Could not load revenue by category.'),
    });

    this.analyticsApi.revenueByTopCustomers(this.range).subscribe({
      next: (data) => this.revenueByTopCustomers.set(data),
      error: () => this.errorMessage.set('Could not load revenue by top customers.'),
    });
  }

  onRangeChange(change: DateRangeChange): void {
    this.range = { from: change.from ?? undefined, to: change.to ?? undefined };
    this.rangeLabel.set(change.from || change.to ? `${change.from ?? 'earliest'} to ${change.to ?? 'latest'}` : 'All available orders');
    this.loadData();
  }

  private buildKpis(): void {
    const summary = this.summary();
    if (!summary) return;
    const sparkline = this.sparkline;
    this.kpis.set([
      { label: 'Total Revenue', value: this.currency(summary.totalRevenue), icon: 'database', deltaLabel: `${summary.totalOrders} orders`, sparkline },
      { label: 'Total Orders', value: `${summary.totalOrders}`, icon: 'trending-up', deltaLabel: `${summary.totalCustomers} customers`, sparkline },
      { label: 'Avg. Order Value', value: this.currency(summary.avgOrderValue), icon: 'box', deltaLabel: `${summary.totalProducts} products in catalog`, sparkline },
      { label: 'Customers', value: `${summary.totalCustomers}`, icon: 'users', deltaLabel: 'from workspace.gold.customer', sparkline: [] },
      { label: 'Top Category', value: summary.topCategoryName, icon: 'layers', deltaLabel: `${summary.topCategorySharePct}% of revenue`, sparkline: [] },
    ]);
  }

  private currency(value: number): string {
    return `$${value.toLocaleString(undefined, { maximumFractionDigits: 0 })}`;
  }

  get topCustomersTotal(): string {
    const total = this.revenueByTopCustomers().reduce((sum, c) => sum + c.value, 0);
    return this.currency(total);
  }

  get topAnomaly(): AnomalyRow | null {
    return this.recentAnomalies()[0] ?? null;
  }

  impactTone(impact: string): 'error' | 'warning' | 'info' {
    if (impact === 'High') return 'error';
    if (impact === 'Medium') return 'warning';
    return 'info';
  }
}
