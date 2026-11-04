import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { KpiCardComponent } from '../../shared/components/kpi-card/kpi-card.component';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { HorizontalBarChartComponent, BarDatum } from '../../shared/components/charts/horizontal-bar-chart.component';
import { DonutChartComponent, DonutDatum } from '../../shared/components/charts/donut-chart.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { PaginationComponent } from '../../shared/components/pagination/pagination.component';
import { SalesApiService, SalesSummary2, SalesOrderDetail, FilterOptions } from '../../core/services/sales-api.service';
import { LabelValue } from '../../core/services/analytics-api.service';

@Component({
  selector: 'app-sales-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    KpiCardComponent,
    PanelComponent,
    IconComponent,
    HorizontalBarChartComponent,
    DonutChartComponent,
    DataTableComponent,
    EmptyStateComponent,
    PaginationComponent,
  ],
  template: `
    <div class="page">
      <div class="filters">
        <div class="filter-field">
          <label>From</label>
          <input type="date" [(ngModel)]="from" [min]="options()?.minOrderDate ?? undefined" [max]="options()?.maxOrderDate ?? undefined" (change)="reload()" />
        </div>
        <div class="filter-field">
          <label>To</label>
          <input type="date" [(ngModel)]="to" [min]="options()?.minOrderDate ?? undefined" [max]="options()?.maxOrderDate ?? undefined" (change)="reload()" />
        </div>
        <div class="filter-field">
          <label>Category</label>
          <select [(ngModel)]="category" (change)="reload()">
            <option [ngValue]="undefined">All categories</option>
            @for (c of options()?.categories ?? []; track c) {
              <option [ngValue]="c">{{ c }}</option>
            }
          </select>
        </div>
        <div class="filter-field">
          <label>Territory</label>
          <select [(ngModel)]="territory" (change)="reload()">
            <option [ngValue]="undefined">All territories</option>
            @for (t of options()?.territories ?? []; track t) {
              <option [ngValue]="t">{{ t }}</option>
            }
          </select>
        </div>
        <button type="button" class="btn-clear" (click)="clearFilters()">Clear filters</button>
        <div class="period-label">{{ summary()?.periodLabel }}</div>
      </div>

      @if (errorMessage()) {
        <app-empty-state icon="alert-triangle" tone="error" title="Could not load the Sales Dashboard" [description]="errorMessage()!" />
      } @else {
        <div class="kpi-grid">
          <app-kpi-card label="Total Revenue" [value]="currency(summary()?.totalRevenue ?? 0)" icon="database" [deltaLabel]="(summary()?.totalOrders ?? 0) + ' orders'" />
          <app-kpi-card label="Total Orders" [value]="(summary()?.totalOrders ?? 0).toString()" icon="box" [deltaLabel]="(summary()?.totalCustomers ?? 0) + ' customers'" />
          <app-kpi-card label="Total Customers" [value]="(summary()?.totalCustomers ?? 0).toString()" icon="users" deltaLabel="in filtered period" />
          <app-kpi-card label="Avg. Order Value" [value]="currency(summary()?.avgOrderValue ?? 0)" icon="trending-up" deltaLabel="per order" />
          <app-kpi-card
            label="Revenue Growth"
            [value]="summary()?.revenueGrowthPct == null ? 'Not available' : (summary()!.revenueGrowthPct! > 0 ? '+' : '') + summary()!.revenueGrowthPct! + '%'"
            icon="trending-up"
            deltaLabel="single-order-date sample: no prior period"
          />
        </div>

        <div class="panel-grid">
          <app-panel title="Revenue by Category">
            @if (revenueByCategory().length) {
              <app-horizontal-bar-chart [data]="revenueByCategory()" valueSuffix="%" />
            } @else {
              <app-empty-state title="No category revenue for this filter" />
            }
          </app-panel>
          <app-panel title="Sales by Territory">
            @if (revenueByTerritory().length) {
              <app-horizontal-bar-chart [data]="territoryBarData()" valuePrefix="$" valueSuffix="" />
            } @else {
              <app-empty-state title="No territory data" description="AdventureWorksLT has no SalesTerritory table — territory is mapped from Address.State_Province." />
            }
          </app-panel>
          <app-panel title="Top-Selling Products">
            @if (topProducts().length) {
              <app-horizontal-bar-chart [data]="productBarData()" valuePrefix="$" valueSuffix="" />
            } @else {
              <app-empty-state title="No product data" />
            }
          </app-panel>
          <app-panel title="Top Customers">
            @if (topCustomers().length) {
              <app-donut-chart [data]="topCustomers()" [clickable]="true" (sliceClick)="goToCustomers($event)" />
            } @else {
              <app-empty-state title="No customer data" />
            }
          </app-panel>
          <app-panel title="Order Status Distribution">
            @if (orderStatus().length) {
              <app-donut-chart [data]="orderStatus()" />
            } @else {
              <app-empty-state title="No order status data" />
            }
          </app-panel>
        </div>

        <app-panel title="Orders (drill-down)">
          <div panelActions>
            <a class="btn-export" [href]="exportUrl()" download>
              <app-icon name="download" [size]="14" />
              Export CSV
            </a>
          </div>
          @if (pagedOrders().length) {
            <app-data-table [columns]="orderColumns" [rows]="pagedOrders()" emptyMessage="No orders match the selected filters." />
            <app-pagination [page]="page()" [pageSize]="pageSize" [totalItems]="orders().length" (pageChange)="page.set($event)" />
          } @else {
            <app-empty-state title="No orders match the selected filters" />
          }
        </app-panel>
      }
    </div>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .filters { display: flex; align-items: flex-end; gap: var(--space-3); flex-wrap: wrap; }
    .filter-field { display: flex; flex-direction: column; gap: 4px; }
    .filter-field label { font-size: var(--font-size-xs); color: var(--color-text-secondary); }
    .filter-field input, .filter-field select {
      background: var(--color-surface-card); border: 1px solid var(--color-border); border-radius: var(--radius-sm);
      color: var(--color-text-primary); padding: 6px var(--space-2); font-size: var(--font-size-sm);
    }
    .btn-clear {
      background: var(--color-surface-card); border: 1px solid var(--color-border); border-radius: var(--radius-sm);
      color: var(--color-text-secondary); padding: 7px var(--space-3); font-size: var(--font-size-sm); cursor: pointer; height: 32px;
    }
    .btn-clear:hover { color: var(--color-text-primary); border-color: var(--color-border-strong); }
    .period-label { margin-left: auto; font-size: var(--font-size-sm); color: var(--color-text-secondary); }
    .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: var(--space-4); }
    .panel-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)); gap: var(--space-4); }
    .btn-export {
      display: inline-flex; align-items: center; gap: 6px; font-size: var(--font-size-sm); color: var(--color-accent);
      text-decoration: none; font-weight: 600;
    }
    .btn-export:hover { color: var(--color-accent-hover); }
    `,
  ],
})
export class SalesDashboardComponent implements OnInit {
  readonly summary = signal<SalesSummary2 | null>(null);
  readonly revenueByCategory = signal<LabelValue[]>([]);
  readonly revenueByTerritory = signal<LabelValue[]>([]);
  readonly topProducts = signal<LabelValue[]>([]);
  readonly topCustomers = signal<LabelValue[]>([]);
  readonly orderStatus = signal<LabelValue[]>([]);
  readonly orders = signal<SalesOrderDetail[]>([]);
  readonly options = signal<FilterOptions | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly page = signal(0);
  readonly pageSize = 10;

  from: string | undefined;
  to: string | undefined;
  category: string | undefined;
  territory: string | undefined;

  readonly orderColumns: TableColumn[] = [
    { key: 'orderNumber', label: 'Order #' },
    { key: 'orderDate', label: 'Order Date' },
    { key: 'customer', label: 'Customer' },
    { key: 'territory', label: 'Territory' },
    { key: 'status', label: 'Status' },
    { key: 'totalDue', label: 'Total Due', align: 'right' },
    { key: 'lineItemCount', label: 'Items', align: 'right' },
  ];

  constructor(private readonly salesApi: SalesApiService, private readonly router: Router) {}

  ngOnInit(): void {
    this.salesApi.filterOptions().subscribe({ next: (o) => this.options.set(o) });
    this.reload();
  }

  reload(): void {
    this.page.set(0);
    const filters = { from: this.from, to: this.to, category: this.category, territory: this.territory };
    this.salesApi.summary(filters).subscribe({ next: (s) => this.summary.set(s), error: () => this.errorMessage.set('Could not load the sales summary from the Spring Boot API.') });
    this.salesApi.revenueByCategory(filters).subscribe({ next: (d) => this.revenueByCategory.set(d) });
    this.salesApi.revenueByTerritory(filters).subscribe({ next: (d) => this.revenueByTerritory.set(d) });
    this.salesApi.topProducts(filters).subscribe({ next: (d) => this.topProducts.set(d) });
    this.salesApi.topCustomers(filters).subscribe({ next: (d) => this.topCustomers.set(d) });
    this.salesApi.orderStatus(filters).subscribe({ next: (d) => this.orderStatus.set(d) });
    this.salesApi.orders(filters).subscribe({ next: (d) => this.orders.set(d) });
  }

  clearFilters(): void {
    this.from = this.to = this.category = this.territory = undefined;
    this.reload();
  }

  pagedOrders(): SalesOrderDetail[] {
    const start = this.page() * this.pageSize;
    return this.orders().slice(start, start + this.pageSize);
  }

  territoryBarData(): BarDatum[] {
    return this.revenueByTerritory();
  }

  productBarData(): BarDatum[] {
    return this.topProducts();
  }

  exportUrl(): string {
    return this.salesApi.exportOrdersUrl({ from: this.from, to: this.to, category: this.category, territory: this.territory });
  }

  goToCustomers(datum: { label: string }): void {
    this.router.navigate(['/analytics/customers'], { queryParams: { search: datum.label } });
  }

  currency(value: number): string {
    return `$${value.toLocaleString(undefined, { maximumFractionDigits: 0 })}`;
  }
}
