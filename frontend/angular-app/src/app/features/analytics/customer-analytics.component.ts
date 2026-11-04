import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { KpiCardComponent } from '../../shared/components/kpi-card/kpi-card.component';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { DonutChartComponent } from '../../shared/components/charts/donut-chart.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { DrawerComponent } from '../../shared/components/drawer/drawer.component';
import {
  CustomerAnalyticsApiService,
  CustomerSummary,
  CustomerRow,
  CustomerDetail,
} from '../../core/services/customer-analytics-api.service';
import { LabelValue } from '../../core/services/analytics-api.service';

@Component({
  selector: 'app-customer-analytics',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    KpiCardComponent,
    PanelComponent,
    BadgeComponent,
    DataTableComponent,
    DonutChartComponent,
    EmptyStateComponent,
    DrawerComponent,
  ],
  template: `
    <div class="page">
      <div class="kpi-grid">
        <app-kpi-card label="Total Customers" [value]="(summary()?.totalCustomers ?? 0).toString()" icon="users" [deltaLabel]="(summary()?.customersWithOrders ?? 0) + ' with orders'" />
        <app-kpi-card label="Returning Customers" [value]="(summary()?.returningCustomers ?? 0).toString()" icon="trending-up" deltaLabel="more than one order" />
        <app-kpi-card label="Inactive Customers" [value]="(summary()?.inactiveCustomers ?? 0).toString()" icon="alert-triangle" deltaLabel="registered, zero orders" />
        <app-kpi-card label="Avg. Purchase Value" [value]="currency(summary()?.avgPurchaseValue ?? 0)" icon="box" deltaLabel="per order, customers with orders" />
        <app-kpi-card label="Avg. Purchase Frequency" [value]="(summary()?.avgOrdersPerCustomer ?? 0).toFixed(1)" icon="activity" deltaLabel="orders per active customer" />
      </div>

      <app-panel title="Geographic Distribution" subtitle="Revenue by Address.State_Province (AdventureWorksLT has no SalesTerritory table)">
        @if (geo().length) {
          <app-donut-chart [data]="geo()" />
        } @else {
          <app-empty-state title="No geographic data" />
        }
      </app-panel>

      <app-panel title="Customers">
        <div class="toolbar">
          <input class="search" type="text" placeholder="Search by company name..." [(ngModel)]="search" (ngModelChange)="reload()" />
          <select [(ngModel)]="segment" (ngModelChange)="reload()">
            <option [ngValue]="undefined">All segments</option>
            <option value="High-value">High-value</option>
            <option value="Frequent">Frequent</option>
            <option value="New">New</option>
            <option value="Standard">Standard</option>
            <option value="Inactive">Inactive</option>
          </select>
          <select [(ngModel)]="sortBy" (ngModelChange)="reload()">
            <option value="revenue">Sort: Lifetime revenue</option>
            <option value="orders">Sort: Order count</option>
            <option value="name">Sort: Company name</option>
          </select>
        </div>
        @if (customers().length) {
          <app-data-table [columns]="columns" [rows]="customers()" emptyMessage="No customers match these filters." [clickableRows]="true" (rowClick)="openDetail($event)" />
        } @else {
          <app-empty-state title="No customers match these filters" />
        }
      </app-panel>
    </div>

    <app-drawer [open]="detail() !== null" [title]="detail()?.companyName ?? ''" (close)="detail.set(null)">
      @if (detail()) {
        <div class="detail-grid">
          <div><span class="detail-label">Segment</span><app-badge [tone]="segmentTone(detail()!.segment)">{{ detail()!.segment }}</app-badge></div>
          <div><span class="detail-label">Territory</span><span>{{ detail()!.territory }}</span></div>
          <div><span class="detail-label">Orders</span><span>{{ detail()!.orderCount }}</span></div>
          <div><span class="detail-label">Lifetime Revenue</span><span>{{ currency(detail()!.lifetimeRevenue) }}</span></div>
          <div><span class="detail-label">Avg. Order Value</span><span>{{ currency(detail()!.avgOrderValue) }}</span></div>
        </div>
        <h4>Order History</h4>
        @if (detail()!.orders.length) {
          <app-data-table [columns]="orderColumns" [rows]="detail()!.orders" />
        } @else {
          <app-empty-state title="No orders for this customer" />
        }
      }
    </app-drawer>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: var(--space-4); }
    .toolbar { display: flex; gap: var(--space-3); margin-bottom: var(--space-3); flex-wrap: wrap; }
    .search, select {
      background: var(--color-surface-card); border: 1px solid var(--color-border); border-radius: var(--radius-sm);
      color: var(--color-text-primary); padding: 7px var(--space-3); font-size: var(--font-size-sm);
    }
    .search { flex: 1; min-width: 220px; }
    .detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-3); margin-bottom: var(--space-4); }
    .detail-label { display: block; font-size: var(--font-size-xs); color: var(--color-text-muted); margin-bottom: 2px; }
    h4 { color: var(--color-text-primary); font-size: var(--font-size-sm); margin: 0 0 var(--space-2); }
    `,
  ],
})
export class CustomerAnalyticsComponent implements OnInit {
  readonly summary = signal<CustomerSummary | null>(null);
  readonly geo = signal<LabelValue[]>([]);
  readonly customers = signal<CustomerRow[]>([]);
  readonly detail = signal<CustomerDetail | null>(null);

  search = '';
  segment: string | undefined;
  sortBy = 'revenue';

  readonly columns: TableColumn[] = [
    { key: 'companyName', label: 'Company' },
    { key: 'territory', label: 'Territory' },
    { key: 'orderCount', label: 'Orders', align: 'right' },
    { key: 'lifetimeRevenue', label: 'Lifetime Revenue', align: 'right' },
    { key: 'avgOrderValue', label: 'Avg. Order Value', align: 'right' },
    { key: 'segment', label: 'Segment' },
  ];

  readonly orderColumns: TableColumn[] = [
    { key: 'orderNumber', label: 'Order #' },
    { key: 'orderDate', label: 'Order Date' },
    { key: 'totalDue', label: 'Total Due', align: 'right' },
    { key: 'itemCount', label: 'Items', align: 'right' },
  ];

  constructor(private readonly api: CustomerAnalyticsApiService, private readonly route: ActivatedRoute) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['search']) {
        this.search = params['search'];
      }
      this.reload();
    });
    this.api.summary().subscribe({ next: (s) => this.summary.set(s) });
    this.api.geographicDistribution().subscribe({ next: (g) => this.geo.set(g) });
  }

  reload(): void {
    this.api.customers(this.search || undefined, this.segment, this.sortBy).subscribe({ next: (c) => this.customers.set(c) });
  }

  openDetail(row: Record<string, any>): void {
    this.api.detail((row as CustomerRow).customerId).subscribe({ next: (d) => this.detail.set(d) });
  }

  segmentTone(segment: string): 'success' | 'warning' | 'info' | 'neutral' {
    if (segment === 'High-value') return 'success';
    if (segment === 'Inactive') return 'warning';
    if (segment === 'Frequent') return 'info';
    return 'neutral';
  }

  currency(value: number): string {
    return `$${value.toLocaleString(undefined, { maximumFractionDigits: 0 })}`;
  }
}
