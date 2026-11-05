import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { DataTableComponent, TableColumn } from '../../shared/components/data-table/data-table.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { MetricsApiService, MetricSummary, MetricDetail } from '../../core/services/metrics-api.service';

@Component({
  selector: 'app-metrics',
  standalone: true,
  imports: [CommonModule, FormsModule, PanelComponent, IconComponent, DataTableComponent, EmptyStateComponent],
  template: `
    <div class="page">
      <app-panel title="Metrics Explorer" [subtitle]="metrics().length + ' metrics from this app\\'s Micrometer registry'">
        <div panelActions>
          <input class="search" type="text" placeholder="Filter metrics…" [(ngModel)]="search" />
          <button type="button" class="btn-secondary" (click)="reload()"><app-icon name="refresh" [size]="14" /> Refresh</button>
        </div>
        @if (filteredMetrics().length) {
          <app-data-table [columns]="columns" [rows]="filteredMetrics()" [clickableRows]="true" (rowClick)="select($event)" />
        } @else {
          <app-empty-state title="No metrics match this filter" />
        }
      </app-panel>

      @if (selected()) {
        <app-panel [title]="selected()!.name" subtitle="Current sample values, by tag combination">
          @if (selected()!.samples.length) {
            <app-data-table [columns]="sampleColumns" [rows]="sampleRows()" />
          } @else {
            <app-empty-state title="No samples for this metric" />
          }
        </app-panel>
      }
    </div>
  `,
  styles: [
    `
    .page { display: flex; flex-direction: column; gap: var(--space-5); }
    .search {
      padding: 6px var(--space-3); border-radius: var(--radius-sm); border: 1px solid var(--color-border);
      background: var(--color-surface-card-hover); color: var(--color-text-primary); font-size: var(--font-size-sm); width: 220px;
    }
    .btn-secondary {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 7px var(--space-4); border-radius: var(--radius-sm); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer;
      background: var(--color-surface-card-hover); border: 1px solid var(--color-border); color: var(--color-text-secondary);
    }
    `,
  ],
})
export class MetricsComponent implements OnInit {
  readonly metrics = signal<MetricSummary[]>([]);
  readonly selected = signal<MetricDetail | null>(null);
  search = '';

  readonly columns: TableColumn[] = [
    { key: 'name', label: 'Metric' },
    { key: 'type', label: 'Type' },
    { key: 'seriesCount', label: 'Series', align: 'right' },
  ];

  readonly sampleColumns: TableColumn[] = [
    { key: 'tagsLabel', label: 'Tags' },
    { key: 'statistic', label: 'Statistic' },
    { key: 'value', label: 'Value', align: 'right' },
  ];

  constructor(private readonly api: MetricsApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.list().subscribe({ next: (m) => this.metrics.set(m) });
  }

  filteredMetrics(): Record<string, unknown>[] {
    const term = this.search.trim().toLowerCase();
    return this.metrics()
      .filter((m) => !term || m.name.toLowerCase().includes(term))
      .map((m) => ({ ...m }));
  }

  select(row: Record<string, any>): void {
    this.api.detail(row['name']).subscribe({ next: (d) => this.selected.set(d) });
  }

  sampleRows(): Record<string, unknown>[] {
    return (this.selected()?.samples ?? []).map((s) => ({
      ...s,
      tagsLabel: Object.entries(s.tags).map(([k, v]) => `${k}=${v}`).join(', ') || '(none)',
      value: Number(s.value.toFixed(4)),
    }));
  }
}
