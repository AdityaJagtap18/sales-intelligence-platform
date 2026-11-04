import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { PanelComponent } from '../../shared/components/panel/panel.component';
import { KpiCardComponent } from '../../shared/components/kpi-card/kpi-card.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { AnalyticsApiService, OrderRow, SalesSummary } from '../../core/services/analytics-api.service';

@Component({
  selector: 'app-sales-explorer',
  standalone: true,
  imports: [CommonModule, FormsModule, PanelComponent, KpiCardComponent, DataTableComponent],
  templateUrl: './sales-explorer.component.html',
  styleUrl: './sales-explorer.component.scss',
})
export class SalesExplorerComponent implements OnInit {
  readonly summary = signal<SalesSummary | null>(null);
  readonly orders = signal<OrderRow[]>([]);
  readonly search = signal('');
  readonly errorMessage = signal<string | null>(null);

  readonly columns: TableColumn[] = [
    { key: 'orderNumber', label: 'Order #' },
    { key: 'orderDate', label: 'Date' },
    { key: 'customerName', label: 'Customer' },
    { key: 'itemCount', label: 'Items', align: 'right' },
    { key: 'totalDueFormatted', label: 'Total Due', align: 'right' },
  ];

  constructor(private readonly analyticsApi: AnalyticsApiService) {}

  ngOnInit(): void {
    this.analyticsApi.salesSummary().subscribe({
      next: (summary) => this.summary.set(summary),
      error: () => this.errorMessage.set('Could not load the sales summary.'),
    });
    this.analyticsApi.orders().subscribe({
      next: (orders) => this.orders.set(orders),
      error: () => this.errorMessage.set('Could not load orders from the Spring Boot API.'),
    });
  }

  get rows() {
    const query = this.search().trim().toLowerCase();
    return this.orders()
      .filter((o) => !query || o.customerName.toLowerCase().includes(query) || o.orderNumber.toLowerCase().includes(query))
      .map((o) => ({ ...o, totalDueFormatted: `$${o.totalDue.toLocaleString(undefined, { maximumFractionDigits: 2 })}` }));
  }
}
